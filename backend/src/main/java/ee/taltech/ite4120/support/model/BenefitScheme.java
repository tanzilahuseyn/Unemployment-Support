package ee.taltech.ite4120.support.model;

import java.time.LocalDate;

/**
 * A benefit scheme — a row of {@code support.support_type}, extended in revision 2
 * with its legal instrument, rule version, effective dates and the facts its rules
 * require.
 */
public class BenefitScheme {

    private String code;
    private String name;
    private String legalBasis;
    private String ruleVersion;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean requiresResidence;
    private boolean requiresUnemployment;
    private boolean requiresNoEmployment;
    private String reviewInfo;

    public boolean requires(CheckType type) {
        return switch (type) {
            case RESIDENCE -> requiresResidence;
            case UNEMPLOYMENT -> requiresUnemployment;
            case EMPLOYMENT -> requiresNoEmployment;
        };
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public boolean isRequiresResidence() { return requiresResidence; }
    public void setRequiresResidence(boolean requiresResidence) { this.requiresResidence = requiresResidence; }
    public boolean isRequiresUnemployment() { return requiresUnemployment; }
    public void setRequiresUnemployment(boolean requiresUnemployment) { this.requiresUnemployment = requiresUnemployment; }
    public boolean isRequiresNoEmployment() { return requiresNoEmployment; }
    public void setRequiresNoEmployment(boolean requiresNoEmployment) { this.requiresNoEmployment = requiresNoEmployment; }
    public String getReviewInfo() { return reviewInfo; }
    public void setReviewInfo(String reviewInfo) { this.reviewInfo = reviewInfo; }
}
