package ee.taltech.ite4120.support.registry.mock;

import ee.taltech.ite4120.support.registry.employment.http.wire.TorTootamineResponse;
import ee.taltech.ite4120.support.registry.population.http.wire.RrElukohtResponse;
import ee.taltech.ite4120.support.registry.unemployment.http.wire.TkTootuResponse;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three imitated registries — canned data served by the application itself, so
 * the course works offline. Enabled only when {@code support.registry.mock-server=true}
 * (the local profile). Each answers in the REGISTRY's vocabulary, with the registry's
 * own data timestamp {@code andmedSeisuga}.
 *
 * <p>The six people are FICTIONAL, one per case (SUPPORT.01 revision 2):
 * <ul>
 *   <li>39001010011 — every fact verified positive;</li>
 *   <li>49002020024 — lives in Tartu: residence verified adverse;</li>
 *   <li>39003030035 — not registered, active employment: both verified adverse;</li>
 *   <li>49004040048 — not registered as unemployed: verified adverse;</li>
 *   <li>39005050059 — registered as unemployed AND employed: inconclusive;</li>
 *   <li>49006060061 — Töötukassa answers 503: unavailable.</li>
 * </ul>
 */
@RestController
@RequestMapping("/mock-registry")
@ConditionalOnProperty(name = "support.registry.mock-server", havingValue = "true")
public class MockSupportRegistriesController {

    private static final String AS_OF = "2026-10-20T08:00:00+03:00";

    private static final Map<String, RrElukohtResponse> POPULATION = Map.of(
            "39001010011", new RrElukohtResponse("39001010011", "Kalle", "Kask", "Näidise 11, Tallinn", "Tallinn", AS_OF),
            "49002020024", new RrElukohtResponse("49002020024", "Liis", "Lepp", "Näidise 22, Tartu", "Tartu", AS_OF),
            "39003030035", new RrElukohtResponse("39003030035", "Mart", "Mets", "Näidise 33, Tallinn", "Tallinn", AS_OF),
            "49004040048", new RrElukohtResponse("49004040048", "Anu", "Allik", "Näidise 44, Tallinn", "Tallinn", AS_OF),
            "39005050059", new RrElukohtResponse("39005050059", "Rein", "Rebane", "Näidise 55, Tallinn", "Tallinn", AS_OF),
            "49006060061", new RrElukohtResponse("49006060061", "Eve", "Ilves", "Näidise 66, Tallinn", "Tallinn", AS_OF));

    private static final Map<String, TkTootuResponse> UNEMPLOYMENT = Map.of(
            "39001010011", new TkTootuResponse("39001010011", true, "2026-08-15", AS_OF),
            "49002020024", new TkTootuResponse("49002020024", true, "2026-07-01", AS_OF),
            "39003030035", new TkTootuResponse("39003030035", false, null, AS_OF),
            "49004040048", new TkTootuResponse("49004040048", false, null, AS_OF),
            "39005050059", new TkTootuResponse("39005050059", true, "2026-09-01", AS_OF));

    /** People for whom Töötukassa is "down", to demonstrate UNAVAILABLE. */
    private static final Set<String> UNEMPLOYMENT_DOWN = Set.of("49006060061");

    private static final Map<String, TorTootamineResponse> EMPLOYMENT = Map.of(
            "39001010011", new TorTootamineResponse("39001010011", false, AS_OF),
            "49002020024", new TorTootamineResponse("49002020024", false, AS_OF),
            "39003030035", new TorTootamineResponse("39003030035", true, AS_OF),
            "49004040048", new TorTootamineResponse("49004040048", false, AS_OF),
            "39005050059", new TorTootamineResponse("39005050059", true, AS_OF),
            "49006060061", new TorTootamineResponse("49006060061", false, AS_OF));

    @GetMapping("/population/{isikukood}")
    public ResponseEntity<RrElukohtResponse> population(@PathVariable String isikukood) {
        return found(POPULATION.get(isikukood));
    }

    @GetMapping("/unemployment/{isikukood}")
    public ResponseEntity<TkTootuResponse> unemployment(@PathVariable String isikukood) {
        if (UNEMPLOYMENT_DOWN.contains(isikukood)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
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
