package ee.taltech.ite4120.support.model;

/**
 * What the worker decides. No check results: the server decides from the stored
 * evidence and copies the legal basis from the scheme.
 *
 * @param exception approve although a required fact is verified adverse
 * @param reasonCode required to reject
 * @param explanation the material facts; required to reject and for an exception
 */
public record DecisionCommand(Outcome outcome, boolean exception, ReasonCode reasonCode, String explanation) {}
