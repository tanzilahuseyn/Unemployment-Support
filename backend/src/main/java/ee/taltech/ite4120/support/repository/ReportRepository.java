package ee.taltech.ite4120.support.repository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.helex.commons.db.repo.BaseRepository;
import org.springframework.stereotype.Repository;

/** Aggregate queries for the summary report: counts, grouped — no rows, no personal codes. */
@Repository
public class ReportRepository extends BaseRepository {

    /** Active applications whose period overlaps [from, to]; null bounds are open. */
    private static final String IN_PERIOD = """
            a.sys_status = 'A'
            and (cast(? as date) is null or a.period_end >= cast(? as date))
            and (cast(? as date) is null or a.period_start <= cast(? as date))""";

    public long total(LocalDate from, LocalDate to) {
        Long n = queryForObject("select count(1) from support.application a where " + IN_PERIOD,
                Long.class, from, from, to, to);
        return n == null ? 0 : n;
    }

    public Map<String, Long> countBy(String column, LocalDate from, LocalDate to) {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbcTemplate.query("select a." + column + " as k, count(1) as n from support.application a where " + IN_PERIOD
                        + " group by a." + column + " order by a." + column,
                rs -> {
                    out.put(rs.getString("k"), rs.getLong("n"));
                }, from, from, to, to);
        return out;
    }

    /** Decisions of the period's applications, counted per outcome, exception and reason code. */
    public Map<String, Long> decisionCounts(LocalDate from, LocalDate to) {
        Map<String, Long> out = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select d.outcome, d.override, d.reason_code, count(1) as n
                  from support.decision d
                  join support.application a on a.id = d.application_id
                 where d.sys_status = 'A'""" + " and " + IN_PERIOD + """

                 group by d.outcome, d.override, d.reason_code""",
                rs -> {
                    long n = rs.getLong("n");
                    out.merge("outcome:" + rs.getString("outcome"), n, Long::sum);
                    if (rs.getBoolean("override")) {
                        out.merge("exception", n, Long::sum);
                    }
                    String reason = rs.getString("reason_code");
                    if (reason != null && "REJECTED".equals(rs.getString("outcome"))) {
                        out.merge("reason:" + reason, n, Long::sum);
                    }
                }, from, from, to, to);
        return out;
    }
}
