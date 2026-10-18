package ee.taltech.ite4120.support.registry.unemployment.xroad.wire;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** WIRE RECORD — the X-Road request to Töötukassa. A TEACHING STAND-IN for the real contract. */
@JacksonXmlRootElement(localName = "tootuParing", namespace = TkTootuRequest.NS)
public record TkTootuRequest(@JacksonXmlProperty(localName = "isikukood", namespace = NS) String isikukood) {

    public static final String NS = "http://tootukassa.x-road.eu";
}
