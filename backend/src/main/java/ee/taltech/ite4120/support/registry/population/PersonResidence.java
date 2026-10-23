package ee.taltech.ite4120.support.registry.population;

import java.time.OffsetDateTime;

/**
 * A person and where they live, in OUR vocabulary. Looked up live, never stored.
 * {@code dataAsOf} is the registry's own "data as of" time, when it gives one.
 */
public record PersonResidence(String isikukood, String firstName, String lastName, String address, String municipality,
        OffsetDateTime dataAsOf) {}
