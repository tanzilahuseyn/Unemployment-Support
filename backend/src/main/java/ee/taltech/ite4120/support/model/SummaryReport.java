package ee.taltech.ite4120.support.model;

import java.time.LocalDate;
import java.util.Map;

/**
 * The summary report — aggregate counts only, never a personal code. Covers the
 * active applications whose period overlaps {@code from}–{@code to} (null = open-ended).
 */
public record SummaryReport(
        LocalDate from,
        LocalDate to,
        long total,
        Map<String, Long> byStatus,
        Map<String, Long> byScheme,
        long approved,
        long rejected,
        long exceptions,
        Map<String, Long> rejectionReasons) {}
