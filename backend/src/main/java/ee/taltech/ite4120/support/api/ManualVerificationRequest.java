package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.model.CheckStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Resolve an unavailable or inconclusive check from another authorised source. */
public class ManualVerificationRequest {

    /** PASS or FAIL — a manual verification records a verified result. */
    @NotNull
    private CheckStatus result;

    /** Where the fact was verified, e.g. "Töötukassa decision letter shown by the applicant". */
    @Size(max = 200)
    private String source;

    @Size(max = 1000)
    private String note;

    public CheckStatus getResult() { return result; }
    public void setResult(CheckStatus result) { this.result = result; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
