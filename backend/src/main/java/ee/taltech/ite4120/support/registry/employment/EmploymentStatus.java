package ee.taltech.ite4120.support.registry.employment;

/** Whether a person has an active employment, in OUR vocabulary. Never stored. */
public record EmploymentStatus(String isikukood, boolean activeEmployment) {}
