package ee.taltech.ite4120.support.registry.population.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — the provider's X-Road response, schema-faithful, Estonian names. */
@JacksonXmlRootElement(localName = "elukohtParingResponse", namespace = RrElukohtRequest.NS)
public record RrElukohtXmlResponse(
        @JacksonXmlProperty(localName = "isikukood", namespace = RrElukohtRequest.NS) String isikukood,
        @JacksonXmlProperty(localName = "eesnimi", namespace = RrElukohtRequest.NS) String eesnimi,
        @JacksonXmlProperty(localName = "perekonnanimi", namespace = RrElukohtRequest.NS) String perekonnanimi,
        @JacksonXmlProperty(localName = "aadress", namespace = RrElukohtRequest.NS) String aadress,
        @JacksonXmlProperty(localName = "omavalitsus", namespace = RrElukohtRequest.NS) String omavalitsus,
        @JacksonXmlProperty(localName = "andmedSeisuga", namespace = RrElukohtRequest.NS) String andmedSeisuga) {}
