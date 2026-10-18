package ee.taltech.ite4120.support.service;

import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.Checks;
import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Turns the three registry answers into PASS / FAIL / UNAVAILABLE. Reads only —
 * nothing here is stored (SUPPORT.01, privacy constraint). A person a registry does
 * not know is a FAIL; a registry that cannot answer is UNAVAILABLE.
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

    public Checks check(String isikukood) {
        return new Checks(residence(isikukood), unemployment(isikukood), employment(isikukood));
    }

    private Checks.Residence residence(String isikukood) {
        try {
            PersonResidence p = population.lookup(isikukood);
            if (p == null) {
                return new Checks.Residence(CheckStatus.FAIL, null, null, null, null);
            }
            CheckStatus status = municipality.equalsIgnoreCase(p.municipality()) ? CheckStatus.PASS : CheckStatus.FAIL;
            return new Checks.Residence(status, p.firstName(), p.lastName(), p.address(), p.municipality());
        } catch (RegistryUnavailableException e) {
            return new Checks.Residence(CheckStatus.UNAVAILABLE, null, null, null, null);
        }
    }

    private Checks.Unemployment unemployment(String isikukood) {
        try {
            UnemploymentRegistration u = unemployment.lookup(isikukood);
            if (u == null) {
                return new Checks.Unemployment(CheckStatus.FAIL, null, null);
            }
            return new Checks.Unemployment(u.registered() ? CheckStatus.PASS : CheckStatus.FAIL,
                    u.registered(), u.registeredSince());
        } catch (RegistryUnavailableException e) {
            return new Checks.Unemployment(CheckStatus.UNAVAILABLE, null, null);
        }
    }

    private Checks.Employment employment(String isikukood) {
        try {
            EmploymentStatus s = employment.lookup(isikukood);
            if (s == null) {
                return new Checks.Employment(CheckStatus.FAIL, null);
            }
            return new Checks.Employment(s.activeEmployment() ? CheckStatus.FAIL : CheckStatus.PASS, s.activeEmployment());
        } catch (RegistryUnavailableException e) {
            return new Checks.Employment(CheckStatus.UNAVAILABLE, null);
        }
    }
}
