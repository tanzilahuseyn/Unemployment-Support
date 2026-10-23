package ee.taltech.ite4120.support.model;

/**
 * Structured rejection reasons. A reason that cites a registry fact needs that
 * fact to be verified adverse (FAIL), not merely unavailable or inconclusive.
 */
public enum ReasonCode {
    NOT_RESIDENT(CheckType.RESIDENCE),
    NOT_REGISTERED_UNEMPLOYED(CheckType.UNEMPLOYMENT),
    ACTIVE_EMPLOYMENT(CheckType.EMPLOYMENT),
    INCOMPLETE_APPLICATION(null),
    OTHER(null);

    private final CheckType requiredFailure;

    ReasonCode(CheckType requiredFailure) {
        this.requiredFailure = requiredFailure;
    }

    /** The check that must be FAIL for this reason, or null when no registry fact is cited. */
    public CheckType requiredFailure() {
        return requiredFailure;
    }
}
