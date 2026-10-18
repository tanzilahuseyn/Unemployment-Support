package ee.taltech.ite4120.support.registry.employment;

/** The port to the Employment Register (töötamise register, EMTA): is this person employed right now? */
public interface EmploymentRegistryAdapter {

    /**
     * @return the status, or {@code null} when the registry does not know the person — a miss
     * @throws ee.taltech.ite4120.support.registry.RegistryUnavailableException when it cannot answer
     */
    EmploymentStatus lookup(String isikukood);
}
