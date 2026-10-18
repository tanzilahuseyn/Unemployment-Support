package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.model.Outcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The decision body. Deliberately has NO check results: the server runs the checks
 * itself and never trusts results sent by a client.
 */
public class DecisionRequest {

    @NotNull
    private Outcome outcome;

    /** Approve although a check did not pass; needs a reason. */
    private boolean override;

    @Size(max = 2000)
    private String reason;

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public boolean isOverride() { return override; }
    public void setOverride(boolean override) { this.override = override; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
