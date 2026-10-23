package ee.taltech.ite4120.support.registry.employment.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — the Employment Register's X-Road response, schema-faithful, Estonian names. */
@JacksonXmlRootElement(localName = "tootamineParingResponse", namespace = TorTootamineRequest.NS)
public record TorTootamineXmlResponse(
        @JacksonXmlProperty(localName = "isikukood", namespace = TorTootamineRequest.NS) String isikukood,
        @JacksonXmlProperty(localName = "aktiivneTootamine", namespace = TorTootamineRequest.NS) Boolean aktiivneTootamine,
        @JacksonXmlProperty(localName = "andmedSeisuga", namespace = TorTootamineRequest.NS) String andmedSeisuga) {}
