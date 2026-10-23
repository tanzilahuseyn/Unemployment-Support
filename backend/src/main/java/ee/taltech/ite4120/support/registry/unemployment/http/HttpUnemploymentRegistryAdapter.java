package ee.taltech.ite4120.support.registry.unemployment.http;

import ee.taltech.ite4120.support.registry.http.RegistryHttpClient;
import ee.taltech.ite4120.support.registry.http.WireTime;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.http.wire.TkTootuResponse;
import java.time.LocalDate;

/** Töötukassa over plain HTTPS+JSON; locally the imitated registry at /mock-registry/unemployment. */
public class HttpUnemploymentRegistryAdapter implements UnemploymentRegistryAdapter {

    private final RegistryHttpClient http;

    public HttpUnemploymentRegistryAdapter(String baseUrl) {
        this.http = new RegistryHttpClient(baseUrl, "Unemployment Insurance Fund");
    }

    @Override
    public UnemploymentRegistration lookup(String isikukood) {
        TkTootuResponse wire = http.get("/unemployment/" + isikukood, TkTootuResponse.class);
        if (wire == null) {
            return null;
        }
        // The mapper — the ONLY place where Estonian wire names become our names.
        LocalDate since = wire.registreerimiseKuupaev() == null ? null : LocalDate.parse(wire.registreerimiseKuupaev());
        return new UnemploymentRegistration(wire.isikukood(), wire.registreeritud(), since, WireTime.parse(wire.andmedSeisuga()));
    }
}
