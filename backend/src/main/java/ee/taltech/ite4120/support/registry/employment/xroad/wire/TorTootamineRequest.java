package ee.taltech.ite4120.support.registry.employment.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — the X-Road request to the Employment Register. A TEACHING STAND-IN for the real contract. */
@JacksonXmlRootElement(localName = "tootamineParing", namespace = TorTootamineRequest.NS)
public record TorTootamineRequest(@JacksonXmlProperty(localName = "isikukood", namespace = NS) String isikukood) {

    public static final String NS = "http://emta.x-road.eu";
}
