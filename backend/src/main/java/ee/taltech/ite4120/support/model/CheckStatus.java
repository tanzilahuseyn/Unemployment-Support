package ee.taltech.ite4120.support.model;

/**
 * The result of one check. PASS is verified positive and FAIL verified adverse;
 * UNAVAILABLE (the registry could not answer) and INCONCLUSIVE (it answered, but
 * the facts cannot be relied on) are both unverified — never proof of ineligibility.
 */
public enum CheckStatus {
    PASS, FAIL, UNAVAILABLE, INCONCLUSIVE;

    public boolean verified() {
        return this == PASS || this == FAIL;
    }
}
