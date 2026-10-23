package ee.taltech.ite4120.support.service;

import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.AuditEvent;
import ee.taltech.ite4120.support.model.BenefitScheme;
import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.CheckType;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.DecisionCommand;
import ee.taltech.ite4120.support.model.Evidence;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.model.Verification;
import ee.taltech.ite4120.support.repository.ApplicationRepository;
import ee.taltech.ite4120.support.repository.DecisionRepository;
import ee.taltech.ite4120.support.repository.EvidenceRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for unemployment support applications — exactly the ones written as
 * business tests 1–10 in docs/src/specifications/SUPPORT.01 (revision 2).
 */
@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final EvidenceRepository evidence;
    private final DecisionRepository decisions;
    private final RegistryCheckService registries;
    private final AuditService audit;

    public ApplicationService(ApplicationRepository applications, EvidenceRepository evidence,
            DecisionRepository decisions, RegistryCheckService registries, AuditService audit) {
        this.applications = applications;
        this.evidence = evidence;
        this.decisions = decisions;
        this.registries = registries;
        this.audit = audit;
    }

    @Transactional
    public QueryResult<Application> search(ApplicationQueryParams params) {
        // Searching welfare cases by personal code is audited: it is how misuse would happen.
        if (params.getTextContains() != null && !params.getTextContains().isBlank()) {
            audit.log("SEARCH", null, "personal code contains " + params.getTextContains().trim());
        }
        return applications.search(params);
    }

    public Application require(Long id) {
        Application a = applications.load(id);
        if (a == null) {
            throw new NotFoundException("application " + id + " not found");
        }
        return a;
    }

    /** One application for a person to look at — audited, unlike internal reads. */
    @Transactional
    public Application view(Long id) {
        Application a = require(id);
        audit.log("VIEW", id, null);
        return a;
    }

    @Transactional
    public Application create(Application a) {
        validate(a);
        checkNoOverlap(a, null);
        Long id = applications.insert(a);
        audit.log("CREATE", id, a.getSupportTypeCode());
        return require(id);
    }

    @Transactional
    public Application update(Long id, Application a) {
        Application existing = requireUndecided(id);
        // The applicant is identity: the personal code does not change.
        a.setApplicantIsikukood(existing.getApplicantIsikukood());
        validate(a);
        checkNoOverlap(a, id);
        a.setId(id);
        if (!applications.updateUndecided(a)) {
            throw alreadyDecided(id);
        }
        audit.log("UPDATE", id, "verification must be redone");
        return require(id);
    }

    @Transactional
    public void withdraw(Long id) {
        requireUndecided(id);
        if (!applications.withdrawUndecided(id)) {
            throw alreadyDecided(id);
        }
        audit.log("WITHDRAW", id, null);
    }

    /**
     * Query the registries the scheme requires, store one evidence record per check,
     * and set the status. The person's name and address are returned, not stored.
     */
    @Transactional
    public Verification verify(Long id) {
        Application a = requireUndecided(id);
        BenefitScheme scheme = scheme(a.getSupportTypeCode());
        RegistryCheckService.Result result = registries.check(a.getApplicantIsikukood(), scheme);
        for (Evidence e : result.evidence().values()) {
            e.setApplicationId(id);
            evidence.insert(e);
            audit.log("REGISTRY_QUERY", id, e.getSource() + ": " + e.getResult());
        }
        ApplicationStatus status = statusFor(scheme, current(id));
        moveTo(id, status);
        return new Verification(status, evidence.current(id), result.person());
    }

    /** The current evidence per check — stored data only. */
    public List<Evidence> evidence(Long id) {
        require(id);
        return evidence.current(id);
    }

    /**
     * Resolve an UNAVAILABLE or INCONCLUSIVE check from another authorised source. The
     * result must be a verified one (PASS or FAIL) and needs the source and a note.
     */
    @Transactional
    public Verification verifyManually(Long id, CheckType type, CheckStatus result, String source, String note) {
        Application a = requireUndecided(id);
        if (type == null || result == null || !result.verified()) {
            throw new ApiClientException("a manual verification records PASS or FAIL for one check");
        }
        if (blank(source) || blank(note)) {
            throw new ApiClientException("source and note are required for a manual verification");
        }
        BenefitScheme scheme = scheme(a.getSupportTypeCode());
        Evidence currentEvidence = current(id).get(type);
        if (!scheme.requires(type) || currentEvidence == null || currentEvidence.getResult().verified()) {
            throw new ConflictException("evidence",
                    "only an unavailable or inconclusive required check can be verified manually");
        }
        Evidence e = new Evidence();
        e.setApplicationId(id);
        e.setCheckType(type);
        e.setResult(result);
        e.setStatusDetail("MANUALLY_VERIFIED");
        e.setSource("MANUAL · " + source.trim());
        e.setRetrievedAt(OffsetDateTime.now());
        e.setManual(true);
        e.setNote(note.trim());
        evidence.insert(e);
        audit.log("MANUAL_VERIFICATION", id, type + ": " + result);
        ApplicationStatus status = statusFor(scheme, current(id));
        moveTo(id, status);
        return new Verification(status, evidence.current(id), null);
    }

    /**
     * Decide from the stored evidence. The legal basis, rule version and review
     * information are copied from the scheme; the evidence used is linked to the decision.
     */
    @Transactional
    public Decision decide(Long id, DecisionCommand cmd) {
        Application a = requireUndecided(id);
        if (cmd == null || cmd.outcome() == null) {
            throw new ApiClientException("outcome is required: APPROVED or REJECTED");
        }
        BenefitScheme scheme = scheme(a.getSupportTypeCode());
        Map<CheckType, Evidence> current = current(id);
        String explanation = blank(cmd.explanation()) ? null : cmd.explanation().trim();
        boolean exception = false;

        if (cmd.outcome() == Outcome.APPROVED) {
            // Unverifiable facts neither approve nor reject anyone.
            if (a.getStatus() == ApplicationStatus.PENDING_VERIFICATION) {
                throw new ApiClientException("the application is pending verification: verify the unavailable or inconclusive checks first");
            }
            if (a.getStatus() != ApplicationStatus.READY_FOR_DECISION) {
                throw new ApiClientException("the application has not been verified yet");
            }
            boolean adverse = requiredTypes(scheme).stream()
                    .anyMatch(t -> current.get(t).getResult() == CheckStatus.FAIL);
            if (adverse) {
                // A verified adverse fact: approval only as a recorded exception.
                if (!cmd.exception() || explanation == null) {
                    throw new ApiClientException("a required fact is verified adverse: approve only as an exception with an explanation");
                }
                exception = true;
            }
        } else {
            if (cmd.reasonCode() == null || explanation == null) {
                throw new ApiClientException("a rejection needs a reason code and an explanation");
            }
            CheckType cited = cmd.reasonCode().requiredFailure();
            if (cited != null) {
                Evidence e = current.get(cited);
                if (e == null || e.getResult() != CheckStatus.FAIL) {
                    throw new ApiClientException(cmd.reasonCode() + " needs a verified adverse " + cited
                            + " result, not " + (e == null ? "no result" : e.getResult()));
                }
            }
        }

        Decision d = new Decision();
        d.setApplicationId(id);
        d.setOutcome(cmd.outcome());
        d.setException(exception);
        d.setReasonCode(cmd.outcome() == Outcome.REJECTED ? cmd.reasonCode() : null);
        d.setExplanation(explanation);
        d.setLegalBasis(scheme.getLegalBasis());
        d.setRuleVersion(scheme.getRuleVersion());
        d.setReviewInfo(scheme.getReviewInfo());
        d.setResidenceCheck(result(current, CheckType.RESIDENCE));
        d.setUnemploymentCheck(result(current, CheckType.UNEMPLOYMENT));
        d.setEmploymentCheck(result(current, CheckType.EMPLOYMENT));

        ApplicationStatus closed = cmd.outcome() == Outcome.APPROVED ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED;
        if (!applications.moveUndecided(id, closed)) {
            throw alreadyDecided(id);
        }
        Long decisionId = decisions.insert(d);
        evidence.linkToDecision(current.values().stream().map(Evidence::getId).toList(), decisionId);
        audit.log("DECISION", id, cmd.outcome() + (exception ? " (exception)" : "")
                + (d.getReasonCode() == null ? "" : " " + d.getReasonCode()));
        return decision(id);
    }

    public Decision decision(Long id) {
        require(id);
        Decision d = decisions.loadByApplication(id);
        if (d == null) {
            throw new NotFoundException("application " + id + " has no decision yet");
        }
        d.setEvidence(evidence.forDecision(d.getId()));
        return d;
    }

    public List<AuditEvent> audit(Long id) {
        require(id);
        return audit.forApplication(id);
    }

    public List<BenefitScheme> schemes() {
        return applications.schemes(LocalDate.now());
    }

    // ---- rules ------------------------------------------------------------------------

    /** PENDING while a required check is unverified; READY when all required checks are verified. */
    private static ApplicationStatus statusFor(BenefitScheme scheme, Map<CheckType, Evidence> current) {
        boolean unresolved = requiredTypes(scheme).stream()
                .anyMatch(t -> current.get(t) == null || !current.get(t).getResult().verified());
        return unresolved ? ApplicationStatus.PENDING_VERIFICATION : ApplicationStatus.READY_FOR_DECISION;
    }

    private static List<CheckType> requiredTypes(BenefitScheme scheme) {
        return List.of(CheckType.values()).stream().filter(scheme::requires).toList();
    }

    private Map<CheckType, Evidence> current(Long id) {
        return evidence.current(id).stream().collect(Collectors.toMap(Evidence::getCheckType, Function.identity()));
    }

    private static CheckStatus result(Map<CheckType, Evidence> current, CheckType type) {
        Evidence e = current.get(type);
        return e == null ? null : e.getResult();
    }

    private void moveTo(Long id, ApplicationStatus status) {
        if (!applications.moveUndecided(id, status)) {
            throw alreadyDecided(id);
        }
    }

    private BenefitScheme scheme(String code) {
        BenefitScheme s = code == null ? null : applications.scheme(code);
        if (s == null) {
            throw new ApiClientException("unknown benefit scheme " + code);
        }
        return s;
    }

    private Application requireUndecided(Long id) {
        Application a = require(id);
        if (a.getStatus().decided()) {
            throw alreadyDecided(id);
        }
        return a;
    }

    private static ConflictException alreadyDecided(Long id) {
        return new ConflictException("application", "application " + id + " is already decided and can no longer change");
    }

    private void checkNoOverlap(Application a, Long excludeId) {
        // Business rule: one undecided application per person for overlapping periods.
        Long existing = applications.undecidedOverlap(a.getApplicantIsikukood(), a.getPeriodStart(), a.getPeriodEnd(), excludeId);
        if (existing != null) {
            throw new ConflictException("application", "this person already has an undecided application for an "
                    + "overlapping period: application " + existing + " — amend it instead");
        }
    }

    private void validate(Application a) {
        // Business rule: a valid Estonian personal code — format, birth date and checksum.
        if (!PersonalCode.isValid(a.getApplicantIsikukood())) {
            throw new ApiClientException("applicantIsikukood is not a valid Estonian personal code");
        }
        if (a.getPeriodStart() == null || a.getPeriodEnd() == null) {
            throw new ApiClientException("periodStart and periodEnd are required");
        }
        // Business rule: a period does not end before it starts.
        if (a.getPeriodEnd().isBefore(a.getPeriodStart())) {
            throw new ApiClientException("periodEnd must not be before periodStart");
        }
        if (a.getHouseholdSize() == null || a.getHouseholdSize() < 1 || a.getHouseholdSize() > 20) {
            throw new ApiClientException("householdSize must be between 1 and 20");
        }
        scheme(a.getSupportTypeCode());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
