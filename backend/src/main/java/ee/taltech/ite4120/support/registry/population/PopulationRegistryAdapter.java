package ee.taltech.ite4120.support.registry.population;

/**
 * The port to the Population Register (Rahvastikuregister): where does this person
 * live? Which transport answers is configuration ({@code support.registry.mode}).
 */
public interface PopulationRegistryAdapter {

    /**
     * @return the person, or {@code null} when the registry does not know them — a miss
     * @throws ee.taltech.ite4120.support.registry.RegistryUnavailableException when it cannot answer
     */
    PersonResidence lookup(String isikukood);
}
