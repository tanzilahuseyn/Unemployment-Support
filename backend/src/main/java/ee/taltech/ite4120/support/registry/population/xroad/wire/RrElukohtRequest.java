package ee.taltech.ite4120.support.registry.population.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — the X-Road request for a person's residence. A TEACHING STAND-IN for the real contract. */
@JacksonXmlRootElement(localName = "elukohtParing", namespace = RrElukohtRequest.NS)
public record RrElukohtRequest(@JacksonXmlProperty(localName = "isikukood", namespace = NS) String isikukood) {

    public static final String NS = "http://rr.x-road.eu";
}
