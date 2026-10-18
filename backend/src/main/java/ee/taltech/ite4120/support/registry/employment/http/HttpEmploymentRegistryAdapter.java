package ee.taltech.ite4120.support.registry.employment.http;

import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.employment.http.wire.TorTootamineResponse;
import ee.taltech.ite4120.support.registry.http.RegistryHttpClient;

/** Employment Register over plain HTTPS+JSON; locally the imitated registry at /mock-registry/employment. */
public class HttpEmploymentRegistryAdapter implements EmploymentRegistryAdapter {

    private final RegistryHttpClient http;

    public HttpEmploymentRegistryAdapter(String baseUrl) {
        this.http = new RegistryHttpClient(baseUrl, "Employment Register");
    }

    @Override
    public EmploymentStatus lookup(String isikukood) {
        TorTootamineResponse wire = http.get("/employment/" + isikukood, TorTootamineResponse.class);
        // The mapper — the ONLY place where Estonian wire names become our names.
        return wire == null ? null : new EmploymentStatus(wire.isikukood(), wire.aktiivneTootamine());
    }
}
