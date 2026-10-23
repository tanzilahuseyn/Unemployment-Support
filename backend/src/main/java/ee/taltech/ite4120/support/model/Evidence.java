package ee.taltech.ite4120.support.model;

import java.time.OffsetDateTime;

/**
 * One stored check result — maps onto {@code support.check_evidence}. A minimal
 * snapshot: never a name, address or full registry answer.
 */
public class Evidence {

    private Long id;
    private Long applicationId;
    private CheckType checkType;
    private CheckStatus result;
    /** Short returned status, e.g. REGISTERED_SINCE 2026-08-15. */
    private String statusDetail;
    private String source;
    private OffsetDateTime retrievedAt;
    /** The registry's own "data as of" time; null if unknown or manual. */
    private OffsetDateTime sourceDataAt;
    private boolean manual;
    private String note;
    private Long decisionId;
    /** Who verified (the platform column sys_created_by). */
    private String verifiedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public CheckType getCheckType() { return checkType; }
    public void setCheckType(CheckType checkType) { this.checkType = checkType; }
    public CheckStatus getResult() { return result; }
    public void setResult(CheckStatus result) { this.result = result; }
    public String getStatusDetail() { return statusDetail; }
    public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public OffsetDateTime getRetrievedAt() { return retrievedAt; }
    public void setRetrievedAt(OffsetDateTime retrievedAt) { this.retrievedAt = retrievedAt; }
    public OffsetDateTime getSourceDataAt() { return sourceDataAt; }
    public void setSourceDataAt(OffsetDateTime sourceDataAt) { this.sourceDataAt = sourceDataAt; }
    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getDecisionId() { return decisionId; }
    public void setDecisionId(Long decisionId) { this.decisionId = decisionId; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
}
