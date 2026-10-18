package ee.taltech.ite4120.support.registry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ee.taltech.ite4120.support.registry.employment.xroad.XRoadEmploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.population.xroad.XRoadPopulationRegistryAdapter;
import ee.taltech.ite4120.support.registry.unemployment.xroad.XRoadUnemploymentRegistryAdapter;
import ee.taltech.ite4120.support.registry.xroad.XRoadRegistryClient.XRoadSettings;
import org.junit.jupiter.api.Test;

/**
 * The three forge-xroad skeletons are not expected to reach a security server in this
 * course — but each must fail FAST and CLEARLY, as RegistryUnavailableException, so the
 * check becomes UNAVAILABLE. Same contract as animals' XRoadAdapterSkeletonTest.
 */
class XRoadSkeletonsTest {

    /** Reserved discard port — nothing listens here. */
    private static final XRoadSettings NOWHERE =
            new XRoadSettings("http://127.0.0.1:9", "ee-test", "EDU", "taltech", "ite4120");

    @Test
    void populationFailsFastAndClearly() {
        assertThatThrownBy(() -> new XRoadPopulationRegistryAdapter(NOWHERE).lookup("39001010011"))
                .isInstanceOf(RegistryUnavailableException.class)
                .hasMessageContaining("X-Road call failed");
    }

    @Test
    void unemploymentFailsFastAndClearly() {
        assertThatThrownBy(() -> new XRoadUnemploymentRegistryAdapter(NOWHERE).lookup("39001010011"))
                .isInstanceOf(RegistryUnavailableException.class)
                .hasMessageContaining("X-Road call failed");
    }

    @Test
    void employmentFailsFastAndClearly() {
        assertThatThrownBy(() -> new XRoadEmploymentRegistryAdapter(NOWHERE).lookup("39001010011"))
                .isInstanceOf(RegistryUnavailableException.class)
                .hasMessageContaining("X-Road call failed");
    }
}
