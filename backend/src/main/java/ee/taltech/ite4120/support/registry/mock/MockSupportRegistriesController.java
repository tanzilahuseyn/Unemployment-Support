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
 * <p>The six people are FICTIONAL (names, codes and addresses are made up), one per
 * case (SUPPORT.01 revision 2):
 * <ul>
 *   <li>38703140274 — every fact verified positive;</li>
 *   <li>49407225134 — lives in Tartu: residence verified adverse;</li>
 *   <li>37911052485 — not registered, active employment: both verified adverse;</li>
 *   <li>48302283762 — not registered as unemployed: verified adverse;</li>
 *   <li>36809190424 — registered as unemployed AND employed: inconclusive;</li>
 *   <li>60106117391 — Töötukassa answers 503: unavailable.</li>
 * </ul>
 */
@RestController
@RequestMapping("/mock-registry")
@ConditionalOnProperty(name = "support.registry.mock-server", havingValue = "true")
public class MockSupportRegistriesController {

    private static final String AS_OF = "2026-10-20T08:00:00+03:00";

    private static final Map<String, RrElukohtResponse> POPULATION = Map.of(
            "38703140274", new RrElukohtResponse("38703140274", "Kalle Johannes", "Kask", "Ehitajate tee 108-14, Tallinn", "Tallinn", AS_OF),
            "49407225134", new RrElukohtResponse("49407225134", "Liis Maria", "Lepp", "Riia 52-7, Tartu", "Tartu", AS_OF),
            "37911052485", new RrElukohtResponse("37911052485", "Mart", "Mets", "Sõpruse puiestee 200-33, Tallinn", "Tallinn", AS_OF),
            "48302283762", new RrElukohtResponse("48302283762", "Anu Kristiina", "Allik", "Mustamäe tee 46-9, Tallinn", "Tallinn", AS_OF),
            "36809190424", new RrElukohtResponse("36809190424", "Rein", "Rebane", "Pärnu maantee 139-12, Tallinn", "Tallinn", AS_OF),
            "60106117391", new RrElukohtResponse("60106117391", "Eve Liisa", "Ilves", "Tammsaare tee 83-41, Tallinn", "Tallinn", AS_OF));

    private static final Map<String, TkTootuResponse> UNEMPLOYMENT = Map.of(
            "38703140274", new TkTootuResponse("38703140274", true, "2026-08-15", AS_OF),
            "49407225134", new TkTootuResponse("49407225134", true, "2026-07-01", AS_OF),
            "37911052485", new TkTootuResponse("37911052485", false, null, AS_OF),
            "48302283762", new TkTootuResponse("48302283762", false, null, AS_OF),
            "36809190424", new TkTootuResponse("36809190424", true, "2026-09-01", AS_OF));

    /** People for whom Töötukassa is "down", to demonstrate UNAVAILABLE. */
    private static final Set<String> UNEMPLOYMENT_DOWN = Set.of("60106117391");

    private static final Map<String, TorTootamineResponse> EMPLOYMENT = Map.of(
            "38703140274", new TorTootamineResponse("38703140274", false, AS_OF),
            "49407225134", new TorTootamineResponse("49407225134", false, AS_OF),
            "37911052485", new TorTootamineResponse("37911052485", true, AS_OF),
            "48302283762", new TorTootamineResponse("48302283762", false, AS_OF),
            "36809190424", new TorTootamineResponse("36809190424", true, AS_OF),
            "60106117391", new TorTootamineResponse("60106117391", false, AS_OF));

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
