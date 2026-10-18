package ee.taltech.ite4120.support.registry.population;

/** A person and where they live, in OUR vocabulary. Looked up live, never stored. */
public record PersonResidence(String isikukood, String firstName, String lastName, String address, String municipality) {}
