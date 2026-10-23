package ee.taltech.ite4120.support.registry.unemployment;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Whether a person is registered as unemployed, and since when, in OUR vocabulary. Never stored. */
public record UnemploymentRegistration(String isikukood, boolean registered, LocalDate registeredSince,
        OffsetDateTime dataAsOf) {}
