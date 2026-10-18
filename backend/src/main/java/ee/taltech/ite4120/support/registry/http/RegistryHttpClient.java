package ee.taltech.ite4120.support.registry.http;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Plain HTTPS+JSON transport shared by the three HTTP adapters: GET one wire record,
 * 404 is a miss ({@code null}), anything else that is not 200 is the registry being
 * unavailable. Same rules as animals' HttpOwnerRegistryAdapter, written once.
 */
public class RegistryHttpClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String baseUrl;
    private final String registryName;

    public RegistryHttpClient(String baseUrl, String registryName) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.registryName = registryName;
    }

    public <T> T get(String path, Class<T> wireType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return null; // the registry does not know this person — a miss, not a failure
            }
            if (response.statusCode() != 200) {
                throw new RegistryUnavailableException(registryName + " answered HTTP " + response.statusCode());
            }
            return json.readValue(response.body(), wireType);
        } catch (IOException e) {
            throw new RegistryUnavailableException(registryName + " unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistryUnavailableException(registryName + " call interrupted", e);
        }
    }
}
