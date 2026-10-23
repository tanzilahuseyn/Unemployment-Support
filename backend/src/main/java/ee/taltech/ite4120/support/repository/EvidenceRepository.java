package ee.taltech.ite4120.support.repository;

import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.CheckType;
import ee.taltech.ite4120.support.model.Evidence;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

/** Evidence rows: inserted, linked once to the decision that relied on them, never edited. */
@Repository
public class EvidenceRepository extends BaseRepository {

    public Long insert(Evidence e) {
        return queryForObject("""
                insert into support.check_evidence (application_id, check_type, result, status_detail, source,
                                                    retrieved_at, source_data_at, manual, note)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id""",
                Long.class, e.getApplicationId(), e.getCheckType().name(), e.getResult().name(), e.getStatusDetail(),
                e.getSource(), e.getRetrievedAt(), e.getSourceDataAt(), e.isManual(), e.getNote());
    }

    /** The current (latest) evidence per check type of an application. */
    public List<Evidence> current(Long applicationId) {
        return jdbcTemplate.query("""
                select distinct on (e.check_type) e.* from support.check_evidence e
                 where e.application_id = ? and e.sys_status = 'A'
                 order by e.check_type, e.id desc""", EvidenceRepository::map, applicationId);
    }

    public List<Evidence> forDecision(Long decisionId) {
        return jdbcTemplate.query(
                "select e.* from support.check_evidence e where e.decision_id = ? and e.sys_status = 'A' order by e.check_type",
                EvidenceRepository::map, decisionId);
    }

    public void linkToDecision(List<Long> evidenceIds, Long decisionId) {
        for (Long id : evidenceIds) {
            jdbcTemplate.update("update support.check_evidence set decision_id = ? where id = ? and decision_id is null",
                    decisionId, id);
        }
    }

    private static Evidence map(ResultSet rs, int row) throws SQLException {
        Evidence e = new Evidence();
        e.setId(rs.getLong("id"));
        e.setApplicationId(rs.getLong("application_id"));
        e.setCheckType(CheckType.valueOf(rs.getString("check_type")));
        e.setResult(CheckStatus.valueOf(rs.getString("result")));
        e.setStatusDetail(rs.getString("status_detail"));
        e.setSource(rs.getString("source"));
        e.setRetrievedAt(rs.getObject("retrieved_at", OffsetDateTime.class));
        e.setSourceDataAt(rs.getObject("source_data_at", OffsetDateTime.class));
        e.setManual(rs.getBoolean("manual"));
        e.setNote(rs.getString("note"));
        long decisionId = rs.getLong("decision_id");
        e.setDecisionId(rs.wasNull() ? null : decisionId);
        e.setVerifiedBy(rs.getString("sys_created_by"));
        return e;
    }
}
