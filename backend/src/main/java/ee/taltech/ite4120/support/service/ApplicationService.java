package ee.taltech.ite4120.support.service;

import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.Checks;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.model.SupportType;
import ee.taltech.ite4120.support.repository.ApplicationRepository;
import ee.taltech.ite4120.support.repository.DecisionRepository;
import java.util.List;
import java.util.regex.Pattern;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.helex.commons.exception.NotFoundException;
import org.helex.commons.model.QueryResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for unemployment support applications — exactly the ones written as
 * business tests 1–7 in docs/src/specifications/SUPPORT.01.
 */
@Service
public class ApplicationService {

    private static final Pattern ISIKUKOOD = Pattern.compile("\\d{11}");

    private final ApplicationRepository applications;
    private final DecisionRepository decisions;
    private final RegistryCheckService registries;

    public ApplicationService(ApplicationRepository applications, DecisionRepository decisions,
            RegistryCheckService registries) {
        this.applications = applications;
        this.decisions = decisions;
        this.registries = registries;
    }

    public QueryResult<Application> search(ApplicationQueryParams params) {
        return applications.search(params);
    }

    public Application require(Long id) {
        Application a = applications.load(id);
        if (a == null) {
            throw new NotFoundException("application " + id + " not found");
        }
        return a;
    }

    @Transactional
    public Application create(Application a) {
        validate(a);
        checkNoOverlap(a, null);
        return require(applications.insert(a));
    }

    @Transactional
    public Application update(Long id, Application a) {
        Application existing = requireOpen(id);
        // The applicant is identity: the personal code does not change.
        a.setApplicantIsikukood(existing.getApplicantIsikukood());
        validate(a);
        checkNoOverlap(a, id);
        a.setId(id);
        if (!applications.updateOpen(a)) {
            throw alreadyDecided(id);
        }
        return require(id);
    }

    @Transactional
    public void withdraw(Long id) {
        requireOpen(id);
        if (!applications.withdrawOpen(id)) {
            throw alreadyDecided(id);
        }
    }

    /** The three checks, live. Never stored. */
    public Checks checks(Long id) {
        return registries.check(require(id).getApplicantIsikukood());
    }

    public Decision decision(Long id) {
        require(id);
        Decision d = decisions.loadByApplication(id);
        if (d == null) {
            throw new NotFoundException("application " + id + " has no decision yet");
        }
        return d;
    }

    /**
     * Decide an application. The server runs the checks itself — it never trusts
     * results sent by a client — and stores only their outcomes with the decision.
     */
    @Transactional
    public Decision decide(Long id, Outcome outcome, boolean override, String reason) {
        Application a = requireOpen(id);
        if (outcome == null) {
            throw new ApiClientException("outcome is required: APPROVED or REJECTED");
        }
        boolean hasReason = reason != null && !reason.isBlank();
        // Business rule: a rejection needs a reason.
        if (outcome == Outcome.REJECTED && !hasReason) {
            throw new ApiClientException("reason is required to reject");
        }
        Checks checks = registries.check(a.getApplicantIsikukood());
        boolean overriding = false;
        if (outcome == Outcome.APPROVED && !checks.allPass()) {
            // Business rule: a check that did not pass needs an override with a written justification.
            if (!override || !hasReason) {
                throw new ApiClientException("a check did not pass: approve only as an override with a reason");
            }
            overriding = true;
        }
        Decision d = new Decision();
        d.setApplicationId(id);
        d.setOutcome(outcome);
        d.setOverride(overriding);
        d.setReason(hasReason ? reason.trim() : null);
        d.setResidenceCheck(checks.residence().status());
        d.setUnemploymentCheck(checks.unemployment().status());
        d.setEmploymentCheck(checks.employment().status());
        if (!applications.close(id, outcome == Outcome.APPROVED ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED)) {
            throw alreadyDecided(id);
        }
        decisions.insert(d);
        return decisions.loadByApplication(id);
    }

    public List<SupportType> types() {
        return applications.types();
    }

    private Application requireOpen(Long id) {
        Application a = require(id);
        if (a.getStatus() != ApplicationStatus.OPEN) {
            throw alreadyDecided(id);
        }
        return a;
    }

    private static ConflictException alreadyDecided(Long id) {
        return new ConflictException("application", "application " + id + " is already decided and can no longer change");
    }

    private void checkNoOverlap(Application a, Long excludeId) {
        // Business rule: one open application per person for overlapping periods.
        if (applications.openOverlapExists(a.getApplicantIsikukood(), a.getPeriodStart(), a.getPeriodEnd(), excludeId)) {
            throw new ConflictException("application",
                    "this person already has an open application for an overlapping period");
        }
    }

    private void validate(Application a) {
        // Business rule: a personal code is exactly 11 digits — a string, never a number.
        if (a.getApplicantIsikukood() == null || !ISIKUKOOD.matcher(a.getApplicantIsikukood()).matches()) {
            throw new ApiClientException("applicantIsikukood must be exactly 11 digits");
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
        if (a.getSupportTypeCode() == null || !applications.typeExists(a.getSupportTypeCode())) {
            throw new ApiClientException("unknown supportTypeCode " + a.getSupportTypeCode());
        }
    }
}
