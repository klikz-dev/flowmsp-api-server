package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*10.03.00 05/26/21 EMS-Medical 3330 W 177TH ST SUITE 1A; ADVOCATE MEDICAL,HAZ HCFD 27-2 HCFD A119 MENTAL STATUS CHANGE SUITE 1A CONSC ABLE TO SPEAK */


public class ecom8131 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(ecom8131.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("Address: " + parsedMessage.Address);
        log.info("Units: " + parsedMessage.units);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

        try {
            parsedMessage.text = plainMsg;

            var latlon_rc = getLatLon(plainMsg, parsedMessage, msgService);
            parsedMessage.Address = get_address(plainMsg, cust);
            get_units(parsedMessage, msg);

            // if LAT/LON not in message use address
            if (latlon_rc == 0)
                parsedMessage.ErrorFlag = 0;
            else
                //Address should be of at-least 5 characters
                if (parsedMessage.Address.length() > 5) {
                    // Now PlainMsg has Address Only
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
            parsedMessage.ErrorFlag = 1;
        }
        return parsedMessage;
    }

    private static final Pattern[] inputRegexes = new Pattern[16];
    static {
        inputRegexes[0] = Pattern.compile("(EMS-[a-zA-Z]+)(.*)(HAZ)");
        inputRegexes[1] = Pattern.compile("(Lift Asst)(.*)(HAZ)");
        inputRegexes[2] = Pattern.compile("(Accident w Inj)(.*)(HAZ)");
        inputRegexes[3] = Pattern.compile("(Assist PD)(.*)(HAZ)");
        inputRegexes[4] = Pattern.compile("(Alarm-[a-zA-Z]+)(.*)(HAZ)");
        inputRegexes[5] = Pattern.compile("(Elevator Releas)(.*)(HAZ)");
        inputRegexes[6] = Pattern.compile("(Suicidal Subj)(.*)(HAZ)");
        inputRegexes[7] = Pattern.compile("(Wire-[a-zA-Z]+)(.*)(HAZ)");
        inputRegexes[8] = Pattern.compile("(Gas Leak Inside)(.*)(HAZ)");
        inputRegexes[9] = Pattern.compile("(Gas Spill)(.*)(HAZ)");
        inputRegexes[15] = Pattern.compile("(Fire-[a-zA-Z]+)(.*)(HAZ)");
        inputRegexes[10] = Pattern.compile("(CO)(.*)(HAZ)");
        inputRegexes[11] = Pattern.compile("(Odor Invest)(.*)(HAZ)");
        inputRegexes[12] = Pattern.compile("(Assist Other FD)(.*)(HAZ)");
        inputRegexes[13] = Pattern.compile("(Smoke Invest)(.*)(HAZ)");
        inputRegexes[14] = Pattern.compile("(Wash Down)(.*)(HAZ)");
    }

    private static String get_address(String input, Customer cust)
    {
        var addr = "";
        for (Pattern inputRegex : inputRegexes){
            if (inputRegex.matcher(input).find()) {
                var m = inputRegex.matcher(input);
                addr = m.find() ? m.group(2) : "";
                if (addr.length() > 0)
                    addr = addr.replaceAll(";", ", ");
                addr = addr + cust.address.city + ", " + cust.address.state + " " + cust.address.zip;
                return addr;
            }
        }
        return addr;
    }

    public int getLatLon(String msg, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile(".*LAT=([+-]\\d+\\.\\d+) LON=([+-]\\d+\\.\\d+).*");
        var m = p.matcher(msg);

        if (!m.matches()) {
            log.info("getLatLon, pattern match failed");
            return 1;
        }

        log.info("group1=" + m.group(1));
        log.info("group2=" + m.group(2));
        var lat = Double.parseDouble(m.group(1));
        var lon = Double.parseDouble(m.group(2));
        var pos = new Position(lon, lat);
        var point = new Point(pos);

        pm.messageLatLon = point;

        try {
            pm.location = ms.getLocationByPoint(pm.messageLatLon);
        }
        catch (Exception e) {
            log.error("getLocationByPoint failed for " + lat + " " + lon, e);
        }
        log.info(pm.messageLatLon.toString());
        log.info(pm.location.toString());

        return 0;
    }

    private static final String[] inputUnits = new String[18];
    static {
        inputUnits[0] = "A127";
        inputUnits[1] = "A12";
        inputUnits[2] = "E127";
        inputUnits[3] = "E27";
        inputUnits[4] = "T27";
        inputUnits[5] = "B27";
        inputUnits[6] = "C2700";
        inputUnits[7] = "C2701";
        inputUnits[8] = "HAZAM";
        inputUnits[9] = "HAZAC";
        inputUnits[10] = "HAZCH";
        inputUnits[11] = "HAZEN";
        inputUnits[12] = "HAZTK";
        inputUnits[13] = "HCFD";
        inputUnits[14] = "HCFD1";
        inputUnits[15] = "HCFD2";
        inputUnits[16] = "ZCOQA";
        inputUnits[17] = "ZCOQE";
    }

    private void get_units(ParsedMessage pm, String msg) {
        var p = Pattern.compile("(\\d{2}-\\d)(.*)");
        var m = p.matcher(msg);
        var f = m.find() ? m.group(2) : "";

        for (String unit : inputUnits) {
            if (f.trim().contains(unit)) {
                pm.units.add(unit.trim());
            }
        }
    }
}
