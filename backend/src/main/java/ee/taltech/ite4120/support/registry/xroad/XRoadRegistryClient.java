package ee.taltech.ite4120.support.registry.xroad;

import ee.taltech.ite4120.support.registry.RegistryUnavailableException;
import java.net.URI;
import java.time.Duration;
import org.helex.forge.xroad.XRoadClient;
import org.helex.forge.xroad.XRoadClientConfig;
import org.helex.forge.xroad.XRoadClientId;
import org.helex.forge.xroad.XRoadException;
import org.helex.forge.xroad.XRoadResponse;
import org.helex.forge.xroad.XRoadServiceId;

/**
 * The real X-Road transport on {@code org.helex.forge:forge-xroad}, shared by the
 * three X-Road adapters. A SKELETON by design, like animals' XRoadOwnerRegistryAdapter:
 * it builds a correct envelope and fails fast with a clear error when no security
 * server is reachable, which in this course there is not.
 */
public class XRoadRegistryClient {

    private final XRoadClient client;
    private final XRoadServiceId service;
    private final String registryName;

    public XRoadRegistryClient(XRoadSettings settings, String serviceMemberClass, String serviceMemberCode,
            String serviceSubsystem, String serviceCode, String registryName) {
        XRoadClientId clientId = new XRoadClientId(settings.instance(), settings.clientMemberClass(),
                settings.clientMemberCode(), settings.clientSubsystem());
        this.client = new XRoadClient(new XRoadClientConfig(
                URI.create(settings.securityServerUrl()), clientId, "ITE4120", Duration.ofSeconds(30)));
        this.service = new XRoadServiceId(settings.instance(), serviceMemberClass, serviceMemberCode,
                serviceSubsystem, serviceCode, "v1");
        this.registryName = registryName;
    }

    public <T> T send(Object request, Class<T> responseType) {
        try {
            XRoadResponse<T> response = client.send(service, request, responseType);
            return response.body();
        } catch (XRoadException e) {
            throw new RegistryUnavailableException(registryName + ": X-Road call failed: " + e.getMessage(), e);
        }
    }

    /** Who we are on X-Road, and where our security server is. */
    public record XRoadSettings(String securityServerUrl, String instance, String clientMemberClass,
            String clientMemberCode, String clientSubsystem) {}
}
