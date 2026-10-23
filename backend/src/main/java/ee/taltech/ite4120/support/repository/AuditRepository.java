package ee.taltech.ite4120.support.repository;

import ee.taltech.ite4120.support.model.AuditEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

/** The append-only audit log: insert and read; the database refuses anything else. */
@Repository
public class AuditRepository extends BaseRepository {

    public void insert(String actor, String action, Long applicationId, String detail) {
        jdbcTemplate.update(
                "insert into support.audit_event (actor, action, application_id, detail) values (?, ?, ?, ?)",
                actor, action, applicationId, detail);
    }

    public List<AuditEvent> forApplication(Long applicationId) {
        return jdbcTemplate.query(
                "select a.* from support.audit_event a where a.application_id = ? order by a.occurred_at, a.id",
                AuditRepository::map, applicationId);
    }

    private static AuditEvent map(ResultSet rs, int row) throws SQLException {
        AuditEvent e = new AuditEvent();
        e.setId(rs.getLong("id"));
        e.setOccurredAt(rs.getObject("occurred_at", OffsetDateTime.class));
        e.setActor(rs.getString("actor"));
        e.setAction(rs.getString("action"));
        long app = rs.getLong("application_id");
        e.setApplicationId(rs.wasNull() ? null : app);
        e.setDetail(rs.getString("detail"));
        return e;
    }
}
