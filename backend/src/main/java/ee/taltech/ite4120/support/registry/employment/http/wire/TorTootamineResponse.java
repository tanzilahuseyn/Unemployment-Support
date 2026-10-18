package ee.taltech.ite4120.support.registry.employment.http.wire;

/** WIRE RECORD — the Employment Register's answer, Estonian names. */
public record TorTootamineResponse(String isikukood, boolean aktiivneTootamine) {}
