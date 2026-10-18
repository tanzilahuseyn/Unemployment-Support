package ee.taltech.ite4120.support.registry;

import ee.taltech.ite4120.support.registry.employment.EmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.http.HttpEmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.employment.xroad.XRoadEmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.PopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.http.HttpPopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.xroad.XRoadPopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.UnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.http.HttpUnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.xroad.XRoadUnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.xroad.XRoadRegistryClient.XRoadSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the transport behind the three registry ports from configuration:
 * {@code support.registry.mode=http} (default) against {@code support.registry.url} —
 * locally this application's own imitated registries — or {@code xroad}, the
 * forge-xroad skeletons.
 */
@Configuration
public class SupportRegistryConfig {

    private final boolean xroad;
    private final String url;
    private final XRoadSettings settings;

    public SupportRegistryConfig(
            @Value("${support.registry.mode:http}") String mode,
            @Value("${support.registry.url:http://localhost:18440/mock-registry}") String url,
            @Value("${support.registry.xroad.security-server-url:http://localhost:8080}") String ssUrl,
            @Value("${support.registry.xroad.instance:ee-test}") String instance,
            @Value("${support.registry.xroad.client-member-class:EDU}") String memberClass,
            @Value("${support.registry.xroad.client-member-code:taltech}") String memberCode,
            @Value("${support.registry.xroad.client-subsystem:ite4120}") String subsystem) {
        this.xroad = "xroad".equalsIgnoreCase(mode);
        this.url = url;
        this.settings = new XRoadSettings(ssUrl, instance, memberClass, memberCode, subsystem);
    }

    @Bean
    public PopulationRegistryAdapter populationRegistryAdapter() {
        return xroad ? new XRoadPopulationRegistryAdapter(settings) : new HttpPopulationRegistryAdapter(url);
    }

    @Bean
    public UnemploymentRegistryAdapter unemploymentRegistryAdapter() {
        return xroad ? new XRoadUnemploymentRegistryAdapter(settings) : new HttpUnemploymentRegistryAdapter(url);
    }

    @Bean
    public EmploymentRegistryAdapter employmentRegistryAdapter() {
        return xroad ? new XRoadEmploymentRegistryAdapter(settings) : new HttpEmploymentRegistryAdapter(url);
    }
}
