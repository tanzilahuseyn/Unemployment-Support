package ee.taltech.ite4120.support.dto;

import org.helex.commons.model.QueryParams;

/** Query parameters for the application list; the platform QueryParams carries limit, offset and sort. */
public class ApplicationQueryParams extends QueryParams {

    /** Substring match on the applicant's personal code. */
    private String textContains;
    /** OPEN, APPROVED or REJECTED. */
    private String status;
    private String supportTypeCode;

    public String getTextContains() { return textContains; }
    public void setTextContains(String textContains) { this.textContains = textContains; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSupportTypeCode() { return supportTypeCode; }
    public void setSupportTypeCode(String supportTypeCode) { this.supportTypeCode = supportTypeCode; }
}
