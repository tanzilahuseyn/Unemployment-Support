package ee.taltech.ite4120.support.model;

/**
 * The result of one registry check. UNAVAILABLE means the registry could not be
 * reached — viewing fails open, approving fails closed (SUPPORT.01, failure policy).
 */
public enum CheckStatus {
    PASS, FAIL, UNAVAILABLE;

    public boolean passed() {
        return this == PASS;
    }
}
