package ee.taltech.ite4120.support.registry.unemployment.http.wire;

/**
 * WIRE RECORD — Töötukassa's answer, Estonian names. The date arrives as text
 * (YYYY-MM-DD) and is parsed by the mapper in HttpUnemploymentRegistryAdapter.
 */
public record TkTootuResponse(String isikukood, boolean registreeritud, String registreerimiseKuupaev, String andmedSeisuga) {}
