package ee.taltech.ite4120.support.model;

import java.util.List;

/**
 * The answer to a verification: the new status, the stored evidence, and — for the
 * worker's screen only — the person's name and address, which are NOT stored.
 */
public record Verification(ApplicationStatus status, List<Evidence> evidence, Person person) {

    /** Live from the Population Register; shown once, never stored or logged. */
    public record Person(String firstName, String lastName, String address, String municipality) {}
}
