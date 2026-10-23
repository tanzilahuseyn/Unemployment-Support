package ee.taltech.ite4120.support.registry.unemployment.xroad;

import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistration;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.xroad.wire.TkTootuRequest;
import ee.taltech.ite4120.support.registry.unemployment.xroad.wire.TkTootuXmlResponse;
import ee.taltech.ite4120.support.registry.http.WireTime;
import ee.taltech.ite4120.support.registry.xroad.XRoadRegistryClient;
import java.time.LocalDate;

/** Töötukassa over X-Road — a skeleton (see XRoadRegistryClient). */
public class XRoadUnemploymentRegistryAdapter implements UnemploymentRegistryAdapter {

    private final XRoadRegistryClient client;

    public XRoadUnemploymentRegistryAdapter(XRoadRegistryClient.XRoadSettings settings) {
        this.client = new XRoadRegistryClient(settings, "GOV", "70009980", "tootukassa", "tootuParing",
                "Unemployment Insurance Fund");
    }

    @Override
    public UnemploymentRegistration lookup(String isikukood) {
        TkTootuXmlResponse wire = client.send(new TkTootuRequest(isikukood), TkTootuXmlResponse.class);
        if (wire == null || wire.isikukood() == null) {
            return null;
        }
        LocalDate since = wire.registreerimiseKuupaev() == null ? null : LocalDate.parse(wire.registreerimiseKuupaev());
        return new UnemploymentRegistration(wire.isikukood(), Boolean.TRUE.equals(wire.registreeritud()), since,
                WireTime.parse(wire.andmedSeisuga()));
    }
}
