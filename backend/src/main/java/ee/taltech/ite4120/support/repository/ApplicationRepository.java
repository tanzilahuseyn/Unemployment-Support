package ee.taltech.ite4120.support.repository;

import ee.taltech.ite4120.support.dto.ApplicationQueryParams;
import ee.taltech.ite4120.support.model.Application;
import ee.taltech.ite4120.support.model.ApplicationStatus;
import ee.taltech.ite4120.support.model.SupportType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.helex.commons.db.bean.PgBeanProcessor;
import org.helex.commons.db.repo.BaseRepository;
import org.helex.commons.db.sql.SqlBuilder;
import org.helex.commons.exception.ApiClientException;
import org.helex.commons.model.QueryResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Data access for applications — the shape of AnimalRepository. Rows are mapped by
 * hand because the status column maps onto an enum.
 */
@Repository
public class ApplicationRepository extends BaseRepository {

    private static final String SELECT = "select a.* from support.application a ";

    /** The sort keys the list endpoint accepts and their columns. */
    private static final Map<String, String> SORT = Map.of(
            "periodStart", "a.period_start",
            "applicantIsikukood", "a.applicant_isikukood");

    private static final RowMapper<Application> MAPPER = ApplicationRepository::map;

    private final PgBeanProcessor typeBp = new PgBeanProcessor(SupportType.class);

    public Application load(Long id) {
        List<Application> rows = jdbcTemplate.query(SELECT + "where a.id = ? and a.sys_status = 'A'", MAPPER, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public QueryResult<Application> search(ApplicationQueryParams p) {
        return query(p, this::count, this::list);
    }

    private Integer count(ApplicationQueryParams p) {
        SqlBuilder sb = new SqlBuilder("select count(1) from support.application a where a.sys_status = 'A'");
        sb.append(filter(p));
        return queryForObject(sb.getSql(), Integer.class, sb.getParams());
    }

    private List<Application> list(ApplicationQueryParams p) {
        SqlBuilder sb = new SqlBuilder(SELECT + "where a.sys_status = 'A'");
        sb.append(filter(p));
        sb.append(order(p));
        sb.append(limit(p));
        return jdbcTemplate.query(sb.getSql(), MAPPER, sb.getParams());
    }

    /** An unknown sort key is the caller's fault: 400 with the allowed keys, not a 500. */
    private SqlBuilder order(ApplicationQueryParams p) {
        try {
            return order(p, SORT);
        } catch (IllegalArgumentException e) {
            throw new ApiClientException("unknown sort key; sortable: " + String.join(", ", SORT.keySet().stream().sorted().toList()));
        }
    }

    private SqlBuilder filter(ApplicationQueryParams p) {
        SqlBuilder sb = new SqlBuilder();
        if (p.getTextContains() != null && !p.getTextContains().isBlank()) {
            sb.and("a.applicant_isikukood like ?", "%" + p.getTextContains().trim() + "%");
        }
        sb.appendIfNotNull("and a.status = ?", p.getStatus());
        sb.appendIfNotNull("and a.support_type_code = ?", p.getSupportTypeCode());
        return sb;
    }

    /** Is there another OPEN application of this person whose period overlaps [start, end]? */
    public boolean openOverlapExists(String isikukood, LocalDate start, LocalDate end, Long excludeId) {
        Integer n = queryForObject("""
                select count(1) from support.application a
                 where a.sys_status = 'A' and a.status = 'OPEN' and a.applicant_isikukood = ?
                   and a.period_start <= ? and a.period_end >= ? and a.id <> coalesce(?, -1)""",
                Integer.class, isikukood, end, start, excludeId);
        return n != null && n > 0;
    }

    public Long insert(Application a) {
        return queryForObject("""
                insert into support.application (applicant_isikukood, support_type_code, period_start, period_end, household_size)
                values (?, ?, ?, ?, ?)
                returning id""",
                Long.class, a.getApplicantIsikukood(), a.getSupportTypeCode(), a.getPeriodStart(), a.getPeriodEnd(),
                a.getHouseholdSize());
    }

    /** Updates an OPEN application only — a decided one is closed. */
    public boolean updateOpen(Application a) {
        return jdbcTemplate.update("""
                update support.application
                   set support_type_code = ?, period_start = ?, period_end = ?, household_size = ?
                 where id = ? and sys_status = 'A' and status = 'OPEN'""",
                a.getSupportTypeCode(), a.getPeriodStart(), a.getPeriodEnd(), a.getHouseholdSize(), a.getId()) == 1;
    }

    /** Withdraw — a soft delete, OPEN applications only. */
    public boolean withdrawOpen(Long id) {
        return jdbcTemplate.update(
                "update support.application set sys_status = 'C' where id = ? and sys_status = 'A' and status = 'OPEN'",
                id) == 1;
    }

    /** OPEN → the decided status; false when it was no longer open (a concurrent decision). */
    public boolean close(Long id, ApplicationStatus status) {
        return jdbcTemplate.update(
                "update support.application set status = ? where id = ? and sys_status = 'A' and status = 'OPEN'",
                status.name(), id) == 1;
    }

    public List<SupportType> types() {
        return getBeans("select t.* from support.support_type t order by t.name", typeBp);
    }

    public boolean typeExists(String code) {
        Integer n = queryForObject("select count(1) from support.support_type where code = ?", Integer.class, code);
        return n != null && n > 0;
    }

    private static Application map(ResultSet rs, int row) throws SQLException {
        Application a = new Application();
        a.setId(rs.getLong("id"));
        a.setApplicantIsikukood(rs.getString("applicant_isikukood"));
        a.setSupportTypeCode(rs.getString("support_type_code"));
        a.setPeriodStart(rs.getObject("period_start", LocalDate.class));
        a.setPeriodEnd(rs.getObject("period_end", LocalDate.class));
        a.setHouseholdSize(rs.getInt("household_size"));
        a.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        a.setSysStatus(rs.getString("sys_status"));
        a.setSysVersion(rs.getLong("sys_version"));
        a.setSysCreatedAt(rs.getObject("sys_created_at", OffsetDateTime.class));
        a.setSysCreatedBy(rs.getString("sys_created_by"));
        a.setSysModifiedAt(rs.getObject("sys_modified_at", OffsetDateTime.class));
        a.setSysModifiedBy(rs.getString("sys_modified_by"));
        return a;
    }
}
