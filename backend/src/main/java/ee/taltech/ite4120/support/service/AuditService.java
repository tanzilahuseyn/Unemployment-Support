package ee.taltech.ite4120.support.service;

import ee.taltech.ite4120.config.JdbcConfig;
import ee.taltech.ite4120.support.model.AuditEvent;
import ee.taltech.ite4120.support.repository.AuditRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Writes the append-only audit log. The actor is the signed-in user — the same
 * principal JdbcConfig hands to the database. Details are short texts; callers must
 * never pass a name, address or registry payload.
 */
@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void log(String action, Long applicationId, String detail) {
        repository.insert(JdbcConfig.currentUser(), action, applicationId, detail);
    }

    public List<AuditEvent> forApplication(Long applicationId) {
        return repository.forApplication(applicationId);
    }
}
