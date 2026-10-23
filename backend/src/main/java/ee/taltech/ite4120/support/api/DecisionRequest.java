package ee.taltech.ite4120.support.api;

import ee.taltech.ite4120.support.model.DecisionCommand;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.model.ReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The decision body. Deliberately has NO check results: the server decides from the
 * stored evidence and never trusts results sent by a client.
 */
public class DecisionRequest {

    @NotNull
    private Outcome outcome;

    /** Approve although a required fact is verified adverse; needs an explanation. */
    private boolean exception;

    /** Required to reject. */
    private ReasonCode reasonCode;

    /** The material facts. Required to reject and for an exception. */
    @Size(max = 2000)
    private String explanation;

    public DecisionCommand toCommand() {
        return new DecisionCommand(outcome, exception, reasonCode, explanation);
    }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public boolean isException() { return exception; }
    public void setException(boolean exception) { this.exception = exception; }
    public ReasonCode getReasonCode() { return reasonCode; }
    public void setReasonCode(ReasonCode reasonCode) { this.reasonCode = reasonCode; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
