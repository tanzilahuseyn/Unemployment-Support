package ee.taltech.ite4120.support.registry.mock;

import ee.taltech.ite4120.support.registry.employment.http.wire.TorTootamineResponse;
import ee.taltech.ite4120.support.registry.population.http.wire.RrElukohtResponse;
import ee.taltech.ite4120.support.registry.unemployment.http.wire.TkTootuResponse;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three imitated registries — canned data served by the application itself, so
 * the course works offline. Enabled only when {@code support.registry.mock-server=true}
 * (the local profile). Each answers in the REGISTRY's vocabulary, so the adapters
 * exercise the same mapping they would against the real thing.
 *
 * <p>The four people are FICTIONAL, one per case (SUPPORT.01, The registries):
 * 39001010011 passes all three checks; 49002020022 lives in another municipality;
 * 39003030033 has an active employment; 49004040044 is not registered as unemployed.
 */
@RestController
@RequestMapping("/mock-registry")
@ConditionalOnProperty(name = "support.registry.mock-server", havingValue = "true")
public class MockSupportRegistriesController {

    private static final Map<String, RrElukohtResponse> POPULATION = Map.of(
            "39001010011", new RrElukohtResponse("39001010011", "Kalle", "Kask", "Näidise 11, Tallinn", "Tallinn"),
            "49002020022", new RrElukohtResponse("49002020022", "Liis", "Lepp", "Näidise 22, Tartu", "Tartu"),
            "39003030033", new RrElukohtResponse("39003030033", "Mart", "Mets", "Näidise 33, Tallinn", "Tallinn"),
            "49004040044", new RrElukohtResponse("49004040044", "Anu", "Allik", "Näidise 44, Tallinn", "Tallinn"));

    private static final Map<String, TkTootuResponse> UNEMPLOYMENT = Map.of(
            "39001010011", new TkTootuResponse("39001010011", true, "2026-08-15"),
            "49002020022", new TkTootuResponse("49002020022", true, "2026-07-01"),
            "39003030033", new TkTootuResponse("39003030033", true, "2026-09-10"),
            "49004040044", new TkTootuResponse("49004040044", false, null));

    private static final Map<String, TorTootamineResponse> EMPLOYMENT = Map.of(
            "39001010011", new TorTootamineResponse("39001010011", false),
            "49002020022", new TorTootamineResponse("49002020022", false),
            "39003030033", new TorTootamineResponse("39003030033", true),
            "49004040044", new TorTootamineResponse("49004040044", false));

    @GetMapping("/population/{isikukood}")
    public ResponseEntity<RrElukohtResponse> population(@PathVariable String isikukood) {
        return found(POPULATION.get(isikukood));
    }

    @GetMapping("/unemployment/{isikukood}")
    public ResponseEntity<TkTootuResponse> unemployment(@PathVariable String isikukood) {
        return found(UNEMPLOYMENT.get(isikukood));
    }

    @GetMapping("/employment/{isikukood}")
    public ResponseEntity<TorTootamineResponse> employment(@PathVariable String isikukood) {
        return found(EMPLOYMENT.get(isikukood));
    }

    private static <T> ResponseEntity<T> found(T body) {
        return body == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(body);
    }
}
