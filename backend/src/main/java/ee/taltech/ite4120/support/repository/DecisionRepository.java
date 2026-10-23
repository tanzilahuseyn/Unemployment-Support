package ee.taltech.ite4120.support.repository;

import ee.taltech.ite4120.support.model.CheckStatus;
import ee.taltech.ite4120.support.model.Decision;
import ee.taltech.ite4120.support.model.Outcome;
import ee.taltech.ite4120.support.model.ReasonCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

/** Data access for decisions: one per application, written once, never updated. */
@Repository
public class DecisionRepository extends BaseRepository {

    public Decision loadByApplication(Long applicationId) {
        List<Decision> rows = jdbcTemplate.query(
                "select d.* from support.decision d where d.application_id = ? and d.sys_status = 'A'",
                DecisionRepository::map, applicationId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Long insert(Decision d) {
        return queryForObject("""
                insert into support.decision (application_id, outcome, override, reason, reason_code, legal_basis,
                                              rule_version, review_info,
                                              residence_check, unemployment_check, employment_check)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                returning id""",
                Long.class, d.getApplicationId(), d.getOutcome().name(), d.isException(), d.getExplanation(),
                d.getReasonCode() == null ? null : d.getReasonCode().name(), d.getLegalBasis(), d.getRuleVersion(),
                d.getReviewInfo(), name(d.getResidenceCheck()), name(d.getUnemploymentCheck()),
                name(d.getEmploymentCheck()));
    }

    private static String name(CheckStatus s) {
        return s == null ? null : s.name();
    }

    private static CheckStatus status(String s) {
        return s == null ? null : CheckStatus.valueOf(s);
    }

    private static Decision map(ResultSet rs, int row) throws SQLException {
        Decision d = new Decision();
        d.setId(rs.getLong("id"));
        d.setApplicationId(rs.getLong("application_id"));
        d.setOutcome(Outcome.valueOf(rs.getString("outcome")));
        d.setException(rs.getBoolean("override"));
        String code = rs.getString("reason_code");
        d.setReasonCode(code == null ? null : ReasonCode.valueOf(code));
        d.setExplanation(rs.getString("reason"));
        d.setLegalBasis(rs.getString("legal_basis"));
        d.setRuleVersion(rs.getString("rule_version"));
        d.setReviewInfo(rs.getString("review_info"));
        d.setResidenceCheck(status(rs.getString("residence_check")));
        d.setUnemploymentCheck(status(rs.getString("unemployment_check")));
        d.setEmploymentCheck(status(rs.getString("employment_check")));
        d.setSysCreatedAt(rs.getObject("sys_created_at", OffsetDateTime.class));
        d.setSysCreatedBy(rs.getString("sys_created_by"));
        return d;
    }
}
