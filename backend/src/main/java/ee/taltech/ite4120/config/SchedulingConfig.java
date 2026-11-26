package ee.taltech.ite4120.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs — the automatic retry of unavailable registry checks. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
