package ee.taltech.ite4120.support.model;

import java.time.OffsetDateTime;

/** One row of the append-only {@code support.audit_event} log. */
public class AuditEvent {

    private Long id;
    private OffsetDateTime occurredAt;
    private String actor;
    private String action;
    private Long applicationId;
    private String detail;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
