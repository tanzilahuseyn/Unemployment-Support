package ee.taltech.ite4120.support.registry.population.xroad;

import ee.taltech.ite4120.support.registry.population.PersonResidence;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.xroad.wire.RrElukohtRequest;
import ee.taltech.ite4120.support.registry.population.xroad.wire.RrElukohtXmlResponse;
import ee.taltech.ite4120.support.registry.http.WireTime;
import ee.taltech.ite4120.support.registry.xroad.XRoadRegistryClient;

/** Population Register over X-Road — a skeleton (see XRoadRegistryClient). */
public class XRoadPopulationRegistryAdapter implements PopulationRegistryAdapter {

    private final XRoadRegistryClient client;

    public XRoadPopulationRegistryAdapter(XRoadRegistryClient.XRoadSettings settings) {
        this.client = new XRoadRegistryClient(settings, "GOV", "70008440", "rr", "elukohtParing", "Population Register");
    }

    @Override
    public PersonResidence lookup(String isikukood) {
        RrElukohtXmlResponse wire = client.send(new RrElukohtRequest(isikukood), RrElukohtXmlResponse.class);
        if (wire == null || wire.isikukood() == null) {
            return null;
        }
        return new PersonResidence(wire.isikukood(), wire.eesnimi(), wire.perekonnanimi(), wire.aadress(), wire.omavalitsus(),
                WireTime.parse(wire.andmedSeisuga()));
    }
}
