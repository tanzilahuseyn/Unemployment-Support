package ee.taltech.ite4120.support;

import static org.assertj.core.api.Assertions.assertThat;

import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Business test 14 of docs/src/specifications/SUPPORT.01 — the citizen e-service.
 * An HTTP test, because the citizen and worker roles are enforced at the HTTP layer.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CitizenEServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    /** Valid Estonian personal codes (checksums correct). */
    private static final String X = "38703140274";
    private static final String Y = "49407225134";

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    PopulationRegistryAdapter population;

    @MockitoBean
    UnemploymentRegistryAdapter unemployment;

    @MockitoBean
    EmploymentRegistryAdapter employment;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void aCitizenAppliesOnlineOnlyForThemselves() throws Exception {
        String body = """
                {"applicantIsikukood":"%s","supportTypeCode":"MONTHLY",
                 "periodStart":"2034-02-01","periodEnd":"2034-02-28","householdSize":2}""".formatted(Y);

        // When citizen X applies with a body that names Y
        HttpResponse<String> created = post("/api/citizen/applications", "citizen:" + X, body);

        // Then 201, and the application belongs to X, is OPEN, and names citizen:X as its author
        assertThat(created.statusCode()).isEqualTo(201);
        Map<String, Object> row = jdbc.queryForMap(
                "select id, applicant_isikukood, status, sys_created_by from support.application "
                        + "where period_start = date '2034-02-01' and sys_status = 'A'");
        assertThat(row.get("applicant_isikukood")).isEqualTo(X);
        assertThat(row.get("status")).isEqualTo("OPEN");
        assertThat(row.get("sys_created_by")).isEqualTo("citizen:" + X);
        assertThat(jdbc.queryForObject("select count(1) from support.application where applicant_isikukood = ? and period_start = date '2034-02-01'",
                Integer.class, Y)).isZero();
        assertThat(jdbc.queryForObject(
                "select actor from support.audit_event where application_id = ? and action = 'CREATE'",
                String.class, row.get("id"))).isEqualTo("citizen:" + X);

        // Applying again for an overlapping period conflicts
        assertThat(post("/api/citizen/applications", "citizen:" + X, body).statusCode()).isEqualTo(409);

        // A citizen may not use the worker API, and a worker may not use the citizen API
        assertThat(get("/api/support/applications", "citizen:" + X).statusCode()).isEqualTo(403);
        assertThat(post("/api/citizen/applications", "superadmin", body).statusCode()).isEqualTo(403);

        // A citizen token with an invalid personal code is not signed in
        assertThat(get("/api/citizen/schemes", "citizen:12345678901").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> post(String path, String token, String json) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
