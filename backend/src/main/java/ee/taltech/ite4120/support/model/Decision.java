package ee.taltech.ite4120.support.model;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The decision on one application — maps onto {@code support.decision}. Who decided
 * and when are {@code sysCreatedBy} / {@code sysCreatedAt}. Revision 2 adds the
 * structured reason, the legal basis, rule version and review information copied
 * from the scheme, and the evidence the decision relied on.
 */
public class Decision {

    private Long id;
    private Long applicationId;
    private Outcome outcome;
    /** Approved although a required fact was verified adverse (column {@code override}). */
    private boolean exception;
    private ReasonCode reasonCode;
    /** The material facts (column {@code reason}). */
    private String explanation;
    private String legalBasis;
    private String ruleVersion;
    private String reviewInfo;
    /** Result per check at the moment of deciding; null when the scheme does not require it. */
    private CheckStatus residenceCheck;
    private CheckStatus unemploymentCheck;
    private CheckStatus employmentCheck;
    private List<Evidence> evidence;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public boolean isException() { return exception; }
    public void setException(boolean exception) { this.exception = exception; }
    public ReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getReviewInfo() { return reviewInfo; }
    public void setReviewInfo(String reviewInfo) { this.reviewInfo = reviewInfo; }
    public CheckStatus getResidenceCheck() { return residenceCheck; }
    public void setResidenceCheck(CheckStatus residenceCheck) { this.residenceCheck = residenceCheck; }
    public CheckStatus getUnemploymentCheck() { return unemploymentCheck; }
    public void setUnemploymentCheck(CheckStatus unemploymentCheck) { this.unemploymentCheck = unemploymentCheck; }
    public CheckStatus getEmploymentCheck() { return employmentCheck; }
    public void setEmploymentCheck(CheckStatus employmentCheck) { this.employmentCheck = employmentCheck; }
    public List<Evidence> getEvidence() { return evidence; }
    public void setEvidence(List<Evidence> evidence) { this.evidence = evidence; }
    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }
    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }
}
