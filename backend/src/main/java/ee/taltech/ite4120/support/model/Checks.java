package ee.taltech.ite4120.support.model;

import java.time.LocalDate;

/**
 * The three live registry checks of one application. Built on request and shown
 * to the worker; never stored — a decision keeps only the three statuses.
 */
public record Checks(Residence residence, Unemployment unemployment, Employment employment) {

    public boolean allPass() {
        return residence.status().passed() && unemployment.status().passed() && employment.status().passed();
    }

    /** Population Register: lives in this municipality. Details are null when the person is unknown or the registry is down. */
    public record Residence(CheckStatus status, String firstName, String lastName, String address, String municipality) {}

    /** Unemployment Insurance Fund: registered as unemployed, and since when. */
    public record Unemployment(CheckStatus status, Boolean registered, LocalDate registeredSince) {}

    /** Employment Register: no active employment. */
    public record Employment(CheckStatus status, Boolean activeEmployment) {}
}
