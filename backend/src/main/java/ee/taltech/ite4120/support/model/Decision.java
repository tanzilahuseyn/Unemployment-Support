package ee.taltech.ite4120.support.model;

import java.time.OffsetDateTime;

/**
 * The decision on one application. Maps onto {@code support.decision} — see
 * 03-decision.sql. Who decided and when are {@code sysCreatedBy} / {@code sysCreatedAt}.
 * The checks are kept only as their outcome at the moment of deciding.
 */
public class Decision {

    private Long id;
    private Long applicationId;
    private Outcome outcome;
    /** True only for an approval although a check did not pass. */
    private boolean override;
    /** Required when rejecting and when overriding. */
    private String reason;
    private CheckStatus residenceCheck;
    private CheckStatus unemploymentCheck;
    private CheckStatus employmentCheck;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public boolean isOverride() { return override; }
    public void setOverride(boolean override) { this.override = override; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public CheckStatus getResidenceCheck() { return residenceCheck; }
    public void setResidenceCheck(CheckStatus residenceCheck) { this.residenceCheck = residenceCheck; }
    public CheckStatus getUnemploymentCheck() { return unemploymentCheck; }
    public void setUnemploymentCheck(CheckStatus unemploymentCheck) { this.unemploymentCheck = unemploymentCheck; }
    public CheckStatus getEmploymentCheck() { return employmentCheck; }
    public void setEmploymentCheck(CheckStatus employmentCheck) { this.employmentCheck = employmentCheck; }
    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }
    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }
}
