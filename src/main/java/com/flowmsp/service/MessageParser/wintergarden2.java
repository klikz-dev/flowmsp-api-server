package com.flowmsp.service.MessageParser;

import java.io.StringReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import javax.print.Doc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class wintergarden2 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(wintergarden2.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.debug("wintergarden2 msg");
        log.debug(msg);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(msg)));

            XPath xPath = XPathFactory.newInstance().newXPath();

            parsedMessage.incidentID = get_field(xPath, document, "/CadIncident/IncidentNumber");
            parsedMessage.Code = get_field(xPath, document, "/CadIncident/CallNature");
            parsedMessage.Address = get_address(cust, xPath, document);

            var has_latlon = get_latlon(xPath, document, parsedMessage, msgService);

            get_units(xPath, document, parsedMessage);

            String[] lines = {
                    "Incident: " + parsedMessage.incidentID,
                    "Code: " + parsedMessage.Code,
                    "Address: " + parsedMessage.Address
            };

            parsedMessage.text = String.join("\n", lines);

            log.debug("Code: " + parsedMessage.Code);
            log.debug("Address: " + parsedMessage.Address);
            log.debug("Units: " + parsedMessage.units);

            if (has_latlon == 0) {
                log.debug("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
            }

            //Address should be of at-least 5 characters
            else if (parsedMessage.Address.length() > 5) {
                Location location = msgService.getLocationByAddress(parsedMessage.Address);
                Point latLon = null;
                if (location == null) {
                    latLon = msgService.getPointByGoogleMaps(cust, parsedMessage.Address);
                    if (latLon != null) {
                        location = msgService.getLocationByPoint(latLon);
                    }
                } else {
                    // this should be the centre of location polygon
                    latLon = PolygonUtil.getCenter(location.geoOutline);
                }
                if (location != null) {
                    parsedMessage.location = location;
                }
                if (latLon != null) {
                    parsedMessage.messageLatLon = latLon;
                }
            }

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private String get_field(XPath xpath, Document doc, String expr) throws javax.xml.xpath.XPathExpressionException {
        var nodeList = (NodeList) xpath.compile(expr).evaluate(doc, XPathConstants.NODESET);

        var i = nodeList.item(0).getChildNodes().item(0);

        if (i == null)
            return "";
        else
            return i.getNodeValue();
    }

    private String get_address(Customer cust, XPath xpath, Document doc) throws javax.xml.xpath.XPathExpressionException {
        var addr = get_field(xpath, doc, "/CadIncident/IncidentAddress1");
        var city = get_field(xpath, doc, "/CadIncident/IncidentCity");
        var state = get_field(xpath, doc, "/CadIncident/IncidentState");

        if (city.length() == 0)
            city = cust.address.city;

        if (state.length() == 0)
            state = cust.address.state;

        return addr + ", " + city + ", " + state;
    }

    private int get_units(XPath xpath, Document doc, ParsedMessage pm) throws javax.xml.xpath.XPathExpressionException {
        var expr = "/CadIncident/InnerCadIncidents/CadIncident/EmsUnitCallSign/text()";
        var nodes = (NodeList) xpath.compile(expr).evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            pm.units.add(nodes.item(i).getNodeValue());
        }

        return 0;
    }

    private int get_latlon(XPath xpath, Document doc, ParsedMessage pm, MessageService ms) throws javax.xml.xpath.XPathExpressionException {
        var lat = get_field(xpath, doc, "/CadIncident/SceneGpsLocationLat");
        var lon = get_field(xpath, doc, "/CadIncident/SceneGpsLocationLong");

        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat) ;
            var dlon = Double.parseDouble(lon) ;

            var pos = new Position(dlon, dlat);
            var point = new Point(pos);

            pm.messageLatLon = point;

            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.debug(pm.messageLatLon.toString());
                return 0;
            }
            catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
        }

        return 1;
    }
}
