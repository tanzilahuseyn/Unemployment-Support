package ee.taltech.ite4120.support.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The automatic retry of UNAVAILABLE checks (SUPPORT.01, Failure policy): every
 * {@code support.registry.retry.interval} (default one hour) each pending application
 * with an UNAVAILABLE check and no manual evidence is verified again. It never decides.
 * {@code support.registry.retry.enabled: false} switches it off.
 */
@Component
@ConditionalOnProperty(name = "support.registry.retry.enabled", havingValue = "true", matchIfMissing = true)
public class UnavailableRetryJob {

    private static final Logger log = LoggerFactory.getLogger(UnavailableRetryJob.class);

    private final ApplicationService service;

    public UnavailableRetryJob(ApplicationService service) {
        this.service = service;
    }

    /** One retry round; returns how many applications were verified again. */
    @Scheduled(initialDelayString = "${support.registry.retry.interval:PT1H}",
            fixedDelayString = "${support.registry.retry.interval:PT1H}")
    public int retryUnavailable() {
        int retried = 0;
        for (Long id : service.awaitingRetry()) {
            try {
                if (service.retryAutomatically(id)) {
                    retried++;
                }
            } catch (RuntimeException e) {
                // One application failing (for example decided meanwhile) must not stop the round.
                log.warn("automatic retry of application {} failed: {}", id, e.getMessage());
            }
        }
        if (retried > 0) {
            log.info("automatic retry verified {} application(s) again", retried);
        }
        return retried;
    }
}
