package ee.taltech.ite4120.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.AuditEvent;
import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.CheckType;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.DecisionCommand;
import ee.taltech.ite4120.support.model.Evidence;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.model.ReasonCode;
import ee.taltech.ite4120.support.model.Verification;
import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.service.ApplicationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * THE BUSINESS TESTS — the executable form of tests 1–10 in
 * docs/src/specifications/SUPPORT.01 (revision 2) § Business tests. Written before
 * the implementation; watched fail; then made green.
 *
 * <p>Runs against a real PostgreSQL via Testcontainers. The three registry adapters
 * are test doubles, so every case is set up exactly. Each test uses its own
 * personal codes (with valid checksums): the database is shared across the class.
 */
@Testcontainers
@SpringBootTest(properties = "support.municipality=Tallinn")
@ActiveProfiles("test")
class SupportBusinessRulesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    private static final OffsetDateTime DATA_AS_OF = OffsetDateTime.parse("2026-10-20T08:00:00+03:00");

    @Autowired
    ApplicationService service;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    PopulationRegistryAdapter population;

    @MockitoBean
    UnemploymentRegistryAdapter unemployment;

    @MockitoBean
    EmploymentRegistryAdapter employment;

    @Test
    void aPersonalCodeIsAValidEstonianPersonalCode() {
        // Given a short code, an invalid first digit, month 13, and a wrong checksum
        for (String invalid : List.of("1234567890", "99001010011", "39013010011", "39001010012")) {
            // When registered, then refused
            assertThatThrownBy(() -> service.create(application(invalid, "MONTHLY", octoberStart(), octoberEnd())))
                    .as("personal code %s", invalid)
                    .isInstanceOf(ApiClientException.class)
                    .hasMessageContaining("applicantIsikukood is not a valid Estonian personal code");
        }
        // And a valid one is accepted
        Application ok = service.create(application("39001010011", "MONTHLY", LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
        assertThat(ok.getId()).isNotNull();
    }

    @Test
    void aPeriodDoesNotEndBeforeItStarts() {
        assertThatThrownBy(() -> service.create(
                application(code("4900101001"), "MONTHLY", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1))))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("periodEnd must not be before periodStart");
    }

    @Test
    void oneUndecidedApplicationPerPersonForOverlappingPeriods() {
        // Given an undecided application A for October
        String x = code("4900101002");
        Application a = service.create(application(x, "MONTHLY", octoberStart(), octoberEnd()));

        // When another overlapping one is registered, then it conflicts and names A
        assertThatThrownBy(() -> service.create(
                application(x, "MONTHLY", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("application " + a.getId());

        // After A is decided, an overlapping one is accepted
        service.decide(a.getId(), reject(ReasonCode.OTHER, "Applied by mistake"));
        Application again = service.create(
                application(x, "MONTHLY", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15)));
        assertThat(again.getId()).isNotNull();
    }

    @Test
    void onlyRequiredRegistriesAreAskedAndEvidenceIsRecorded() {
        // Given a ONE_OFF application, whose scheme requires residence and unemployment only
        String c = code("4900101003");
        allChecksPass(c);
        Application a = service.create(application(c, "ONE_OFF", octoberStart(), octoberStart()));

        // When it is verified
        Verification v = service.verify(a.getId());

        // Then the Employment Register is never called
        verifyNoInteractions(employment);
        assertThat(v.status()).isEqualTo(ApplicationStatus.READY_FOR_DECISION);

        // And each stored evidence row has result, source and both timestamps, but no names or addresses
        List<Evidence> evidence = service.evidence(a.getId());
        assertThat(evidence).extracting(Evidence::getCheckType)
                .containsExactlyInAnyOrder(CheckType.RESIDENCE, CheckType.UNEMPLOYMENT);
        for (Evidence e : evidence) {
            assertThat(e.getResult()).isEqualTo(CheckStatus.PASS);
            assertThat(e.getSource()).isNotBlank();
            assertThat(e.getRetrievedAt()).isNotNull();
            assertThat(e.getSourceDataAt()).isEqualTo(DATA_AS_OF);
            assertThat(e.getStatusDetail()).doesNotContain("Test", "Person", "Testi 1");
        }
    }

    @Test
    void unverifiableIsPendingNotIneligible() {
        // Given the unemployment registry is down
        String c = code("4900101004");
        allChecksPass(c);
        when(unemployment.lookup(c)).thenThrow(new RegistryUnavailableException("Töötukassa unreachable"));
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));

        // When a MONTHLY application is verified, then it is pending verification
        Verification v = service.verify(a.getId());
        assertThat(v.status()).isEqualTo(ApplicationStatus.PENDING_VERIFICATION);
        assertThat(byType(service.evidence(a.getId())).get(CheckType.UNEMPLOYMENT).getResult())
                .isEqualTo(CheckStatus.UNAVAILABLE);

        // Approval is refused, and so is a rejection citing the unverified fact
        assertThatThrownBy(() -> service.decide(a.getId(), approve()))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("pending verification");
        assertThatThrownBy(() -> service.decide(a.getId(),
                reject(ReasonCode.NOT_REGISTERED_UNEMPLOYED, "Not registered")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("needs a verified adverse");

        // A manual verification needs a source and a note
        assertThatThrownBy(() -> service.verifyManually(a.getId(), CheckType.UNEMPLOYMENT, CheckStatus.PASS, "", ""))
                .isInstanceOf(ApiClientException.class);

        // After a manual PASS with source and note it is ready, and can be approved
        Verification after = service.verifyManually(a.getId(), CheckType.UNEMPLOYMENT, CheckStatus.PASS,
                "Töötukassa decision letter shown by the applicant", "Registered since 2026-09-01, letter no 123");
        assertThat(after.status()).isEqualTo(ApplicationStatus.READY_FOR_DECISION);
        Decision d = service.decide(a.getId(), approve());
        assertThat(d.getOutcome()).isEqualTo(Outcome.APPROVED);
        assertThat(d.getEvidence()).anySatisfy(e -> {
            assertThat(e.getCheckType()).isEqualTo(CheckType.UNEMPLOYMENT);
            assertThat(e.isManual()).isTrue();
        });
    }

    @Test
    void contradictingRegistriesAreInconclusive() {
        // Given a person registered as unemployed AND with an active employment
        String c = code("4900101005");
        allChecksPass(c);
        when(employment.lookup(c)).thenReturn(new EmploymentStatus(c, true, DATA_AS_OF));
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));

        // When verified, then both checks are inconclusive and the application pending
        Verification v = service.verify(a.getId());
        Map<CheckType, Evidence> e = byType(service.evidence(a.getId()));
        assertThat(e.get(CheckType.UNEMPLOYMENT).getResult()).isEqualTo(CheckStatus.INCONCLUSIVE);
        assertThat(e.get(CheckType.EMPLOYMENT).getResult()).isEqualTo(CheckStatus.INCONCLUSIVE);
        assertThat(e.get(CheckType.RESIDENCE).getResult()).isEqualTo(CheckStatus.PASS);
        assertThat(v.status()).isEqualTo(ApplicationStatus.PENDING_VERIFICATION);
    }

    @Test
    void aVerifiedAdverseFactNeedsAnException() {
        // Given an active employment (and no unemployment registration) for a MONTHLY application
        String c = code("4900101006");
        allChecksPass(c);
        when(unemployment.lookup(c)).thenReturn(new UnemploymentRegistration(c, false, null, DATA_AS_OF));
        when(employment.lookup(c)).thenReturn(new EmploymentStatus(c, true, DATA_AS_OF));
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));
        assertThat(service.verify(a.getId()).status()).isEqualTo(ApplicationStatus.READY_FOR_DECISION);

        // Approval without exception is refused
        assertThatThrownBy(() -> service.decide(a.getId(), approve()))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("approve only as an exception with an explanation");

        // With exception and an explanation it is approved as an exception
        Decision d = service.decide(a.getId(), new DecisionCommand(Outcome.APPROVED, true, null,
                "Employment ended on 30.09; the register is not updated yet"));
        assertThat(d.isException()).isTrue();
        assertThat(d.getEmploymentCheck()).isEqualTo(CheckStatus.FAIL);
    }

    @Test
    void aRejectionIsReasonedAndCarriesTheLegalBasis() {
        // Given residence verified adverse
        String c = code("4900101007");
        allChecksPass(c);
        when(population.lookup(c)).thenReturn(new PersonResidence(c, "Test", "Person", "Testi 1, Tartu", "Tartu", DATA_AS_OF));
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));
        service.verify(a.getId());

        // A rejection without reason code or explanation is refused
        assertThatThrownBy(() -> service.decide(a.getId(), reject(null, "No code")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("a rejection needs a reason code and an explanation");
        assertThatThrownBy(() -> service.decide(a.getId(), reject(ReasonCode.NOT_RESIDENT, " ")))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("a rejection needs a reason code and an explanation");

        // NOT_RESIDENT with residence FAIL is accepted and carries the scheme's legal basis
        Decision d = service.decide(a.getId(), reject(ReasonCode.NOT_RESIDENT, "Lives in Tartu, not in Tallinn"));
        assertThat(d.getReasonCode()).isEqualTo(ReasonCode.NOT_RESIDENT);
        assertThat(d.getLegalBasis()).isNotBlank();
        assertThat(d.getRuleVersion()).isNotBlank();
        assertThat(d.getReviewInfo()).isNotBlank();
    }

    @Test
    void aDecisionIsFinal() {
        String c = code("4900101008");
        allChecksPass(c);
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));
        service.verify(a.getId());
        service.decide(a.getId(), approve());

        assertThatThrownBy(() -> service.decide(a.getId(), reject(ReasonCode.OTHER, "Changed my mind")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.update(a.getId(), application(c, "MONTHLY", octoberStart(), octoberEnd())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.withdraw(a.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void searchesRegistryQueriesAndDecisionsAreAuditedWithoutPayloads() {
        String c = code("4900101009");
        allChecksPass(c);
        Application a = service.create(application(c, "MONTHLY", octoberStart(), octoberEnd()));

        // A search by personal code, a verification and a decision
        ApplicationQueryParams search = new ApplicationQueryParams();
        search.setTextContains(c);
        service.search(search);
        service.verify(a.getId());
        service.decide(a.getId(), approve());

        // each write audit events with the actor and application
        List<AuditEvent> events = service.audit(a.getId());
        assertThat(events).extracting(AuditEvent::getAction)
                .contains("CREATE", "REGISTRY_QUERY", "DECISION");
        assertThat(events).allSatisfy(e -> {
            assertThat(e.getActor()).isNotBlank();
            assertThat(e.getOccurredAt()).isNotNull();
            assertThat(e.getDetail() == null ? "" : e.getDetail()).doesNotContain("Test Person", "Testi 1");
        });
        Integer searches = jdbc.queryForObject(
                "select count(1) from support.audit_event where action = 'SEARCH' and detail like ?",
                Integer.class, "%" + c + "%");
        assertThat(searches).isPositive();

        // The audit log is append-only
        assertThatThrownBy(() -> jdbc.update("update support.audit_event set detail = 'x' where application_id = ?", a.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("delete from support.audit_event where application_id = ?", a.getId()))
                .isInstanceOf(DataAccessException.class);
    }

    /** Lives in Tallinn, registered unemployed, no active employment. Unknown codes are not found. */
    private void allChecksPass(String code) {
        when(population.lookup(anyString())).thenReturn(null);
        when(population.lookup(code)).thenReturn(
                new PersonResidence(code, "Test", "Person", "Testi 1, Tallinn", "Tallinn", DATA_AS_OF));
        when(unemployment.lookup(code)).thenReturn(
                new UnemploymentRegistration(code, true, LocalDate.of(2026, 9, 1), DATA_AS_OF));
        when(employment.lookup(code)).thenReturn(new EmploymentStatus(code, false, DATA_AS_OF));
    }

    private static Map<CheckType, Evidence> byType(List<Evidence> evidence) {
        return evidence.stream().collect(Collectors.toMap(Evidence::getCheckType, Function.identity()));
    }

    private static DecisionCommand approve() {
        return new DecisionCommand(Outcome.APPROVED, false, null, null);
    }

    private static DecisionCommand reject(ReasonCode code, String explanation) {
        return new DecisionCommand(Outcome.REJECTED, false, code, explanation);
    }

    private static Application application(String isikukood, String scheme, LocalDate start, LocalDate end) {
        Application a = new Application();
        a.setApplicantIsikukood(isikukood);
        a.setSupportTypeCode(scheme);
        a.setPeriodStart(start);
        a.setPeriodEnd(end);
        a.setHouseholdSize(2);
        return a;
    }

    /** Ten digits → a personal code with the correct Estonian checksum. */
    private static String code(String tenDigits) {
        int[] w1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 1};
        int[] w2 = {3, 4, 5, 6, 7, 8, 9, 1, 2, 3};
        int s = 0;
        for (int i = 0; i < 10; i++) s += (tenDigits.charAt(i) - '0') * w1[i];
        s %= 11;
        if (s == 10) {
            s = 0;
            for (int i = 0; i < 10; i++) s += (tenDigits.charAt(i) - '0') * w2[i];
            s %= 11;
            if (s == 10) s = 0;
        }
        return tenDigits + s;
    }

    private static LocalDate octoberStart() {
        return LocalDate.of(2026, 10, 1);
    }

    private static LocalDate octoberEnd() {
        return LocalDate.of(2026, 10, 31);
    }
}
