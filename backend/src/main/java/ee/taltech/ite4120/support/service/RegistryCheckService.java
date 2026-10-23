package ee.taltech.ite4120.support.service;

import ee.taltech.ite4120.support.model.BenefitScheme;
import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.CheckType;
import ee.taltech.ite4120.support.model.Evidence;
import ee.taltech.ite4120.support.model.Verification.Person;
import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Asks the registries a scheme REQUIRES — and no others — and turns each answer into
 * an unsaved evidence record (SUPPORT.01 revision 2, Description). The person's name
 * and address are returned for display only; they never enter the evidence.
 */
@Service
public class RegistryCheckService {

    private final PopulationRegistryAdapter population;
    private final UnemploymentRegistryAdapter unemployment;
    private final EmploymentRegistryAdapter employment;
    private final String municipality;

    public RegistryCheckService(PopulationRegistryAdapter population, UnemploymentRegistryAdapter unemployment,
            EmploymentRegistryAdapter employment, @Value("${support.municipality:Tallinn}") String municipality) {
        this.population = population;
        this.unemployment = unemployment;
        this.employment = employment;
        this.municipality = municipality;
    }

    /** Unsaved evidence per required check, plus the live person details for the screen. */
    public record Result(Map<CheckType, Evidence> evidence, Person person) {}

    public Result check(String isikukood, BenefitScheme scheme) {
        Map<CheckType, Evidence> evidence = new EnumMap<>(CheckType.class);
        Person person = null;

        if (scheme.requires(CheckType.RESIDENCE)) {
            try {
                PersonResidence p = population.lookup(isikukood);
                if (p == null) {
                    evidence.put(CheckType.RESIDENCE, evidence(CheckType.RESIDENCE, CheckStatus.INCONCLUSIVE, "NOT_FOUND", null));
                } else {
                    person = new Person(p.firstName(), p.lastName(), p.address(), p.municipality());
                    if (p.municipality() == null || p.municipality().isBlank()) {
                        evidence.put(CheckType.RESIDENCE, evidence(CheckType.RESIDENCE, CheckStatus.INCONCLUSIVE, "NO_MUNICIPALITY", p.dataAsOf()));
                    } else if (municipality.equalsIgnoreCase(p.municipality())) {
                        evidence.put(CheckType.RESIDENCE, evidence(CheckType.RESIDENCE, CheckStatus.PASS, "IN_MUNICIPALITY", p.dataAsOf()));
                    } else {
                        evidence.put(CheckType.RESIDENCE, evidence(CheckType.RESIDENCE, CheckStatus.FAIL, "OTHER_MUNICIPALITY", p.dataAsOf()));
                    }
                }
            } catch (RegistryUnavailableException e) {
                evidence.put(CheckType.RESIDENCE, evidence(CheckType.RESIDENCE, CheckStatus.UNAVAILABLE, null, null));
            }
        }

        if (scheme.requires(CheckType.UNEMPLOYMENT)) {
            try {
                UnemploymentRegistration u = unemployment.lookup(isikukood);
                if (u == null) {
                    evidence.put(CheckType.UNEMPLOYMENT, evidence(CheckType.UNEMPLOYMENT, CheckStatus.FAIL, "NO_RECORD", null));
                } else if (u.registered()) {
                    String since = u.registeredSince() == null ? "REGISTERED" : "REGISTERED_SINCE " + u.registeredSince();
                    evidence.put(CheckType.UNEMPLOYMENT, evidence(CheckType.UNEMPLOYMENT, CheckStatus.PASS, since, u.dataAsOf()));
                } else {
                    evidence.put(CheckType.UNEMPLOYMENT, evidence(CheckType.UNEMPLOYMENT, CheckStatus.FAIL, "NOT_REGISTERED", u.dataAsOf()));
                }
            } catch (RegistryUnavailableException e) {
                evidence.put(CheckType.UNEMPLOYMENT, evidence(CheckType.UNEMPLOYMENT, CheckStatus.UNAVAILABLE, null, null));
            }
        }

        if (scheme.requires(CheckType.EMPLOYMENT)) {
            try {
                EmploymentStatus s = employment.lookup(isikukood);
                if (s == null) {
                    evidence.put(CheckType.EMPLOYMENT, evidence(CheckType.EMPLOYMENT, CheckStatus.PASS, "NO_RECORD", null));
                } else if (s.activeEmployment()) {
                    evidence.put(CheckType.EMPLOYMENT, evidence(CheckType.EMPLOYMENT, CheckStatus.FAIL, "ACTIVE_EMPLOYMENT", s.dataAsOf()));
                } else {
                    evidence.put(CheckType.EMPLOYMENT, evidence(CheckType.EMPLOYMENT, CheckStatus.PASS, "NO_ACTIVE_EMPLOYMENT", s.dataAsOf()));
                }
            } catch (RegistryUnavailableException e) {
                evidence.put(CheckType.EMPLOYMENT, evidence(CheckType.EMPLOYMENT, CheckStatus.UNAVAILABLE, null, null));
            }
        }

        // Registered as unemployed AND actively employed: the registries contradict each other,
        // so neither answer can be relied on until someone checks — inconclusive, not adverse.
        Evidence u = evidence.get(CheckType.UNEMPLOYMENT);
        Evidence e = evidence.get(CheckType.EMPLOYMENT);
        if (u != null && e != null && u.getResult() == CheckStatus.PASS && e.getResult() == CheckStatus.FAIL) {
            u.setResult(CheckStatus.INCONCLUSIVE);
            u.setStatusDetail("CONTRADICTS_EMPLOYMENT_REGISTER");
            e.setResult(CheckStatus.INCONCLUSIVE);
            e.setStatusDetail("CONTRADICTS_UNEMPLOYMENT_REGISTRATION");
        }
        return new Result(evidence, person);
    }

    private static Evidence evidence(CheckType type, CheckStatus result, String detail, OffsetDateTime dataAsOf) {
        Evidence e = new Evidence();
        e.setCheckType(type);
        e.setResult(result);
        e.setStatusDetail(detail);
        e.setSource(type.source());
        e.setRetrievedAt(OffsetDateTime.now());
        e.setSourceDataAt(dataAsOf);
        return e;
    }
}
