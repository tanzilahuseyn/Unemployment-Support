package ee.taltech.ite4120.support.registry.employment;

import java.time.OffsetDateTime;

/** Whether a person has an active employment, in OUR vocabulary. Never stored. */
public record EmploymentStatus(String isikukood, boolean activeEmployment, OffsetDateTime dataAsOf) {}
