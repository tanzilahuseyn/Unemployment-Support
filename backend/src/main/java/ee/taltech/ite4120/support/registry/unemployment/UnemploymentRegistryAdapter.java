package ee.taltech.ite4120.support.registry.unemployment;

/** The port to the Unemployment Insurance Fund (Töötukassa): is this person registered as unemployed? */
public interface UnemploymentRegistryAdapter {

    /**
     * @return the registration, or {@code null} when the registry does not know the person — a miss
     * @throws ee.taltech.ite4120.support.registry.RegistryUnavailableException when it cannot answer
     */
    UnemploymentRegistration lookup(String isikukood);
}
