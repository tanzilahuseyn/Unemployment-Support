package ee.taltech.ite4120.support.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * One application for unemployment support. Maps onto {@code support.application}
 * — see 02-application.sql. Only the personal code identifies the applicant: the
 * registries own everything else about them (SUPPORT.01, privacy constraint).
 */
public class Application {

    private Long id;
    /** Estonian personal code: 11 digits, a STRING, never a number. Immutable after creation. */
    private String applicantIsikukood;
    /** FK → support.support_type(code). */
    private String supportTypeCode;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer householdSize;
    private ApplicationStatus status;

    private String sysStatus;
    private Long sysVersion;
    private OffsetDateTime sysCreatedAt;
    private String sysCreatedBy;
    private OffsetDateTime sysModifiedAt;
    private String sysModifiedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getSysStatus() { return sysStatus; }
    public void setSysStatus(String sysStatus) { this.sysStatus = sysStatus; }
    public Long getSysVersion() { return sysVersion; }
    public void setSysVersion(Long sysVersion) { this.sysVersion = sysVersion; }
    public OffsetDateTime getSysCreatedAt() { return sysCreatedAt; }
    public void setSysCreatedAt(OffsetDateTime sysCreatedAt) { this.sysCreatedAt = sysCreatedAt; }
    public String getSysCreatedBy() { return sysCreatedBy; }
    public void setSysCreatedBy(String sysCreatedBy) { this.sysCreatedBy = sysCreatedBy; }
    public OffsetDateTime getSysModifiedAt() { return sysModifiedAt; }
    public void setSysModifiedAt(OffsetDateTime sysModifiedAt) { this.sysModifiedAt = sysModifiedAt; }
    public String getSysModifiedBy() { return sysModifiedBy; }
    public void setSysModifiedBy(String sysModifiedBy) { this.sysModifiedBy = sysModifiedBy; }
}
