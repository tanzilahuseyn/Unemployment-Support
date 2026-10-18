package ee.taltech.ite4120.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.Checks;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.service.ApplicationService;
import java.time.LocalDate;
import java.util.List;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * THE BUSINESS TESTS — the executable form of tests 1–7 in
 * docs/src/specifications/SUPPORT.01 § Business tests. Written before the
 * implementation; watched fail; then made green.
 *
 * <p>Runs against a real PostgreSQL via Testcontainers, so the support changelog
 * is applied from empty as well. The three registry adapters are replaced by test
 * doubles, so every case — pass, fail, unreachable — is set up exactly. Each test
 * uses its own personal codes: the database is shared across the class.
 */
@Testcontainers
@SpringBootTest(properties = "support.municipality=Tallinn")
@ActiveProfiles("test")
class SupportBusinessRulesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    ApplicationService service;

    @MockitoBean
    PopulationRegistryAdapter population;

    @MockitoBean
    UnemploymentRegistryAdapter unemployment;

    @MockitoBean
    EmploymentRegistryAdapter employment;

    @Test
    void aPersonalCodeIsExactlyElevenDigits() {
        // Given personal codes of ten digits, and of eleven characters with a letter
        for (String malformed : List.of("1234567890", "3810213026A")) {
            Application a = application(malformed, octoberStart(), octoberEnd());

            // When it is registered, then it is refused
            assertThatThrownBy(() -> service.create(a))
                    .as("personal code %s", malformed)
                    .isInstanceOf(ApiClientException.class)
                    .hasMessageContaining("applicantIsikukood must be exactly 11 digits");
        }
    }

    @Test
    void aPeriodDoesNotEndBeforeItStarts() {
        // Given a period from 1 November to 1 October
        Application a = application("49001010012", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1));

        // When it is registered, then it is refused
        assertThatThrownBy(() -> service.create(a))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("periodEnd must not be before periodStart");
    }

    @Test
    void oneOpenApplicationPerPersonForOverlappingPeriods() {
        // Given an open application for person X for October
        String x = "49001010023";
        Application october = service.create(application(x, octoberStart(), octoberEnd()));
        assertThat(october.getStatus()).isEqualTo(ApplicationStatus.OPEN);

        // When a second one for X covers 15 October to 15 November, then it conflicts
        assertThatThrownBy(() -> service.create(
                application(x, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15))))
                .isInstanceOf(ConflictException.class);

        // One for December is accepted
        Application december = service.create(
                application(x, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)));
        assertThat(december.getId()).isNotNull();

        // And when the October application is decided, an overlapping one may be registered again
        service.decide(october.getId(), Outcome.REJECTED, false, "Applied by mistake");
        Application again = service.create(
                application(x, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15)));
        assertThat(again.getId()).isNotNull();
    }

    @Test
    void aRejectionNeedsAReason() {
        // Given an open application
        Application a = service.create(application("49001010034", octoberStart(), octoberEnd()));

        // When it is rejected without a reason, then the decision is refused
        for (String missing : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> service.decide(a.getId(), Outcome.REJECTED, false, missing))
                    .isInstanceOf(ApiClientException.class)
                    .hasMessageContaining("reason is required to reject");
        }
    }

    @Test
    void allChecksPassApproveDirectlyThenItIsClosed() {
        // Given registries that pass all three checks
        String code = "49001010045";
        allChecksPass(code);
        Application a = service.create(application(code, octoberStart(), octoberEnd()));

        // When the application is approved without override
        Decision d = service.decide(a.getId(), Outcome.APPROVED, false, null);

        // Then the decision is stored with outcome APPROVED, no override and three PASS outcomes
        assertThat(d.getOutcome()).isEqualTo(Outcome.APPROVED);
        assertThat(d.isOverride()).isFalse();
        assertThat(d.getResidenceCheck()).isEqualTo(CheckStatus.PASS);
        assertThat(d.getUnemploymentCheck()).isEqualTo(CheckStatus.PASS);
        assertThat(d.getEmploymentCheck()).isEqualTo(CheckStatus.PASS);
        assertThat(service.require(a.getId()).getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        // And a later update, withdrawal or second decision conflicts
        assertThatThrownBy(() -> service.update(a.getId(), application(code, octoberStart(), octoberEnd())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.withdraw(a.getId()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.decide(a.getId(), Outcome.REJECTED, false, "Changed my mind"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aFailingCheckNeedsAnOverrideWithAJustification() {
        // Given registries where the employment check fails
        String code = "49001010056";
        allChecksPass(code);
        when(employment.lookup(code)).thenReturn(new EmploymentStatus(code, true));
        Application a = service.create(application(code, octoberStart(), octoberEnd()));

        // When the application is approved without override, then it is refused
        assertThatThrownBy(() -> service.decide(a.getId(), Outcome.APPROVED, false, null))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("a check did not pass: approve only as an override with a reason");

        // An override needs a reason too
        assertThatThrownBy(() -> service.decide(a.getId(), Outcome.APPROVED, true, " "))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("a check did not pass: approve only as an override with a reason");

        // And when approved with override and a reason, it is stored with override and the FAIL
        Decision d = service.decide(a.getId(), Outcome.APPROVED, true,
                "Employment ended yesterday; the register is not updated yet");
        assertThat(d.getOutcome()).isEqualTo(Outcome.APPROVED);
        assertThat(d.isOverride()).isTrue();
        assertThat(d.getEmploymentCheck()).isEqualTo(CheckStatus.FAIL);
        assertThat(d.getResidenceCheck()).isEqualTo(CheckStatus.PASS);
    }

    @Test
    void anUnreachableRegistryIsUnavailableNotFatal() {
        // Given the unemployment registry is down
        String code = "49001010067";
        allChecksPass(code);
        when(unemployment.lookup(code)).thenThrow(new RegistryUnavailableException("Töötukassa unreachable"));
        Application a = service.create(application(code, octoberStart(), octoberEnd()));

        // When the checks are requested, then unemployment is UNAVAILABLE and the others are present
        Checks checks = service.checks(a.getId());
        assertThat(checks.unemployment().status()).isEqualTo(CheckStatus.UNAVAILABLE);
        assertThat(checks.residence().status()).isEqualTo(CheckStatus.PASS);
        assertThat(checks.employment().status()).isEqualTo(CheckStatus.PASS);

        // And an approval then needs an override
        assertThatThrownBy(() -> service.decide(a.getId(), Outcome.APPROVED, false, null))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("a check did not pass");
        Decision d = service.decide(a.getId(), Outcome.APPROVED, true,
                "Töötukassa confirmed the registration by phone");
        assertThat(d.getUnemploymentCheck()).isEqualTo(CheckStatus.UNAVAILABLE);
        assertThat(d.isOverride()).isTrue();
    }

    /** Lives in Tallinn (the configured municipality), registered unemployed, no active employment. */
    private void allChecksPass(String code) {
        when(population.lookup(anyString())).thenReturn(null);
        when(population.lookup(code)).thenReturn(
                new PersonResidence(code, "Test", "Person", "Testi 1, Tallinn", "Tallinn"));
        when(unemployment.lookup(code)).thenReturn(
                new UnemploymentRegistration(code, true, LocalDate.of(2026, 9, 1)));
        when(employment.lookup(code)).thenReturn(new EmploymentStatus(code, false));
    }

    private static Application application(String isikukood, LocalDate start, LocalDate end) {
        Application a = new Application();
        a.setApplicantIsikukood(isikukood);
        a.setSupportTypeCode("MONTHLY");
        a.setPeriodStart(start);
        a.setPeriodEnd(end);
        a.setHouseholdSize(2);
        return a;
    }

    private static LocalDate octoberStart() {
        return LocalDate.of(2026, 10, 1);
    }

    private static LocalDate octoberEnd() {
        return LocalDate.of(2026, 10, 31);
    }
}
