package ee.taltech.ite4120.support.registry.population.http.wire;

/**
 * WIRE RECORD — the Population Register's answer as the registry defines it. Estonian
 * field names stay Estonian; the mapper in HttpPopulationRegistryAdapter translates.
 */
public record RrElukohtResponse(String isikukood, String eesnimi, String perekonnanimi, String aadress, String omavalitsus,
        String andmedSeisuga) {}
