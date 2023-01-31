package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

/*
18.55.34 11/09/20 EMR MEDIC 704 3RD AVE; 8; ZILLAH GARDENS APARTMENTS,ZIL ZIFD ZI1 ZL0 Z806 Z802 UNIT #8 MOTHER IS LAW IS TRYING TO TAKE OFF HER BURN BANDAGES THEY ARE TRYING TO REDRESS THEM FEMALE HAD DEMENTIA GERTUDIS GONZALES LV FIRE ADV
*/
public class zillahfire implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(zillahfire.class);
    private static final String[] call_types_array = {
	"ACCIDENT INJURY",
	"ACCIDENT UNKNOW",
	"EMR ALARM MED",
	"EMR AMB",
	"EMR IFT",
	"EMR MEDIC",
	"EMR NURSE",
	"EMR RED",
	"EMR YELLOW",
	"EMR LIFT ASSIST",
	"FIRE AIR HEAVY",
	"FIRE AIR LIGHT",
	"FIRE AIR STANDB",
	"FIRE ALARM RES",
	"FIRE APPLIANCE",
	"FIRE AUTO ALM 1",
	"FIRE AUTO ALM 2",
	"FIRE BRUSH GRAS",
	"FIRE CHIMNEY",
	"FIRE ELECTRICAL",
	"FIRE EWR",
	"FIRE FW",
	"FIRE HAYSTACK",
	"FIRE HAZMAT",
	"FIRE INVEST",
	"FIRE OTHER",
	"FIRE POWER PROB",
	"FIRE RESCUE",
	"FIRE SRVC CALL",
	"FIRE STRUC COMM",
	"FIRE STRUCTURE",
	"FIRE TRASH GARB",
	"FIRE VEHICLE",
	"FIRE WILD/URBAN",
	"FIRE WILDFIRE",
	"TEST FIRE",
	"TEST SPILLMAN"
    };
    private static final String call_types = String.join("|", call_types_array);
    private static final String[] city_zone_array = {
	"WAP WPFD", "TOP TPFD", "ZIL ZIFD", "GRA GRFD", "MAB MBFD", "GRV GVFD", "SUN SSFD"};
    private static final String city_zones = String.join("|", city_zone_array);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("zillahfire msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            parsedMessage.text = plainMsg.replace(";", " ");

	    var code = get_code(plainMsg);
	    var city = get_city(plainMsg);

	    var addr_ok = get_address(plainMsg, code, city, parsedMessage);

	    if (code != null)
		parsedMessage.Code = code.group(1);
	    else
		parsedMessage.Code = "";

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (parsedMessage.Address.length() > 5) {
                parsedMessage.ErrorFlag = 0;

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

        } catch (Exception ex) {
	    // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private Matcher get_city(String msg) {
	var p = Pattern.compile(".*(" + city_zones + ").*");
	var m = p.matcher(msg);

	if (m.matches())
	    return m;
	else
	    return null;
    }

    private Matcher get_code(String msg) {
	var p = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{2} \\d{2}/\\d{2}/\\d{2} (" + call_types + ") .*");
	var m = p.matcher(msg);

	if (m.matches())
	    return m;
	else
	    return null;
    }

    private int get_address(String msg, Matcher code, Matcher city, ParsedMessage pm) {
	var addr_start = 0;
	var addr = "";

	// address starts after the end of the code
	if (code != null)
	    addr_start = code.end(1);

	if (city != null)
	    addr = msg.substring(addr_start, city.start(1));
	else
	    addr = msg.substring(addr_start);

	var city_name = "";
	
	if (city != null) {
	    var city_code = city.group(1);

	    if (city_code.equals("WAP WPFD"))
		city_name = "Wapato";
	    else if (city_code.equals("TOP TPFD"))
		city_name = "Toppenish";
	    else if (city_code.equals("ZIL ZIFD"))
		city_name = "Zillah";
	    else if (city_code.equals("GRA GRFD"))
		city_name = "Granger";
	    else if (city_code.equals("MAB MBFD"))
		city_name = "Mabton";
	    else if (city_code.equals("GRV GVFD"))
		city_name = "Grandview";
	    else if (city_code.equals("SUN SSFD"))
		city_name = "Sunnyside";
	}

	log.info("addr=" + addr);
	log.info("city_name=" + city_name);

	pm.Address = (addr + " " + city_name + " WA");

	return 0;
    }
}
