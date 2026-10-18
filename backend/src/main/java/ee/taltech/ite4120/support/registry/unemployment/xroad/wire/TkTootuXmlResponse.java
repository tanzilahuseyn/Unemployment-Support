package ee.taltech.ite4120.support.registry.unemployment.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — Töötukassa's X-Road response, schema-faithful, Estonian names. */
@JacksonXmlRootElement(localName = "tootuParingResponse", namespace = TkTootuRequest.NS)
public record TkTootuXmlResponse(
        @JacksonXmlProperty(localName = "isikukood", namespace = TkTootuRequest.NS) String isikukood,
        @JacksonXmlProperty(localName = "registreeritud", namespace = TkTootuRequest.NS) Boolean registreeritud,
        @JacksonXmlProperty(localName = "registreerimiseKuupaev", namespace = TkTootuRequest.NS) String registreerimiseKuupaev) {}
