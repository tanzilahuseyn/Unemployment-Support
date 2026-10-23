package ee.taltech.ite4120.support.model;

/**
 * OPEN until verified; PENDING_VERIFICATION while a required fact could not be
 * verified; READY_FOR_DECISION when every required fact is verified; then
 * APPROVED or REJECTED, which are final (SUPPORT.01 revision 2, status flow).
 */
public enum ApplicationStatus {
    OPEN, PENDING_VERIFICATION, READY_FOR_DECISION, APPROVED, REJECTED;

    public boolean decided() {
        return this == APPROVED || this == REJECTED;
    }
}
