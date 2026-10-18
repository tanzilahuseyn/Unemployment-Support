package ee.taltech.ite4120.support.registry.population.http;

import ee.taltech.ite4120.support.registry.http.RegistryHttpClient;
import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.http.wire.RrElukohtResponse;

/** Population Register over plain HTTPS+JSON; locally the imitated registry at /mock-registry/population. */
public class HttpPopulationRegistryAdapter implements PopulationRegistryAdapter {

    private final RegistryHttpClient http;

    public HttpPopulationRegistryAdapter(String baseUrl) {
        this.http = new RegistryHttpClient(baseUrl, "Population Register");
    }

    @Override
    public PersonResidence lookup(String isikukood) {
        RrElukohtResponse wire = http.get("/population/" + isikukood, RrElukohtResponse.class);
        // The mapper — the ONLY place where Estonian wire names become our names.
        return wire == null ? null
                : new PersonResidence(wire.isikukood(), wire.eesnimi(), wire.perekonnanimi(), wire.aadress(), wire.omavalitsus());
    }
}
