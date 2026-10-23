package ee.taltech.ite4120.support.registry.employment.xroad;

import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.EmploymentStatus;
import ee.taltech.ite4120.support.registry.employment.xroad.wire.TorTootamineRequest;
import ee.taltech.ite4120.support.registry.employment.xroad.wire.TorTootamineXmlResponse;
import ee.taltech.ite4120.support.registry.http.WireTime;
import ee.taltech.ite4120.support.registry.xroad.XRoadRegistryClient;

/** Employment Register over X-Road — a skeleton (see XRoadRegistryClient). */
public class XRoadEmploymentRegistryAdapter implements EmploymentRegistryAdapter {

    private final XRoadRegistryClient client;

    public XRoadEmploymentRegistryAdapter(XRoadRegistryClient.XRoadSettings settings) {
        this.client = new XRoadRegistryClient(settings, "GOV", "70000349", "emta", "tootamineParing",
                "Employment Register");
    }

    @Override
    public EmploymentStatus lookup(String isikukood) {
        TorTootamineXmlResponse wire = client.send(new TorTootamineRequest(isikukood), TorTootamineXmlResponse.class);
        if (wire == null || wire.isikukood() == null) {
            return null;
        }
        return new EmploymentStatus(wire.isikukood(), Boolean.TRUE.equals(wire.aktiivneTootamine()),
                WireTime.parse(wire.andmedSeisuga()));
    }
}
