package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.model.Application;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * The register/update request body. Bean Validation checks the shape at the edge;
 * the state-dependent rules (period order, overlap, known type) live in ApplicationService.
 */
public class ApplicationRequest {

    /** 11 digits — and a string, never a number. Ignored on update: identity does not change. */
    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "applicantIsikukood must be exactly 11 digits")
    private String applicantIsikukood;

    @NotBlank
    private String supportTypeCode;

    @NotNull
    private LocalDate periodStart;

    @NotNull
    private LocalDate periodEnd;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer householdSize;

    public Application toModel() {
        Application a = new Application();
        a.setApplicantIsikukood(applicantIsikukood);
        a.setSupportTypeCode(supportTypeCode);
        a.setPeriodStart(periodStart);
        a.setPeriodEnd(periodEnd);
        a.setHouseholdSize(householdSize);
        return a;
    }

    public String getApplicantIsikukood() { return applicantIsikukood; }
    public void setApplicantIsikukood(String applicantIsikukood) { this.applicantIsikukood = applicantIsikukood; }
    public String getSupportTypeCode() { return supportTypeCode; }
    public void setSupportTypeCode(String supportTypeCode) { this.supportTypeCode = supportTypeCode; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public Integer getHouseholdSize() { return householdSize; }
    public void setHouseholdSize(Integer householdSize) { this.householdSize = householdSize; }
}
