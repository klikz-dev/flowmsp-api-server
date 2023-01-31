package com.flowmsp.service.MessageParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreatorEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(StreatorEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
	ParsedMessage parsedMessage = new ParsedMessage();
	parsedMessage.ErrorFlag = 99;

        log.info("StreatorEmail msg");
	log.info(msg);

		try {
			/*INCIDENT # 18-2639
			
			LONG TERM CAD#    144462   ACTIVE CALL# 174
			PRIORITY: 0                   REPORTED: 21:41:27 08/08/18
			Determinants/Desc:
			
			
			
			 Nature: MEDICAL AID                                 Type: f
			Address: 41 STANTON PL                               Zone: STRT
			   City: STREATOR
			
			Responding Units: SC6
			
			Directions:
			
			
			Cross Streets:
			   RIDGE RD & LOWDEN RD, W STANTON ST
			
			Lat= 41.130251        Lon= -88.847951
			
			Comments:
			FALL DETECTION ALARM NO CONTACT / GARAGE DOOR CODE 0098 / DOES HAVE DOGS THAT DO
			BITE
			
			Contact: LIFELINE                          Phone: (888)289-2018*/
			
			String plainMsg = msg;			
			plainMsg = plainMsg.replace("+", " ");
			plainMsg = plainMsg.replace("%3A", ":");
			plainMsg = plainMsg.replace("%2F", "/");
			plainMsg = plainMsg.replace("%2C", ",");
			plainMsg = plainMsg.replace("%2E", ".");
			plainMsg = plainMsg.replace("\r\n", " ");
			plainMsg = plainMsg.replace("\n", " ");
			
			int idx = -1;
			int endIndex = -1;
				
			idx = plainMsg.indexOf("Msg-");
			if (idx >= 0) {
				plainMsg = plainMsg.replace("Msg-", "");
				plainMsg = plainMsg.substring(idx);
				plainMsg = plainMsg.trim();
			}
			int NatureIndex = plainMsg.indexOf("Nature:");
			int TypeIndex = plainMsg.indexOf("Type:");
			int AddressIndex = plainMsg.indexOf("Address:");
			int ZoneIndex = plainMsg.indexOf("Zone:");
			int CityIndex = plainMsg.indexOf("City:");
			int RespondingUnitsIndex = plainMsg.indexOf("Responding Units:");
			int CommentIndex = plainMsg.indexOf("Comments:");
			int ReportedIndex = plainMsg.indexOf("REPORTED:");
			int DescIndex = plainMsg.indexOf("Determinants/Desc:");
			
			String nature = "";
			String addressStr = "";
			String commentStr = "";
			String reportStr = "";
			//NATURE
			if (NatureIndex >= 0 && NatureIndex < TypeIndex) {
				nature = plainMsg.substring(NatureIndex + 8, TypeIndex);
				nature = nature.trim();
			}
			//ADDRESS
			if (AddressIndex > TypeIndex && AddressIndex < ZoneIndex ) {
				addressStr = plainMsg.substring(AddressIndex + 8, ZoneIndex);
				addressStr = addressStr.trim();
			}			
			
			// Final ADDRESS with CITY
			if(CityIndex > ZoneIndex && CityIndex < RespondingUnitsIndex){
				String city = plainMsg.substring(CityIndex + 5, RespondingUnitsIndex);  
				addressStr = addressStr + " " + city;
				addressStr = addressStr.trim();
			}
			
			if (ReportedIndex > 0) {
				if (DescIndex > ReportedIndex && DescIndex < NatureIndex) {
					reportStr = plainMsg.substring(ReportedIndex + 9, DescIndex);	
				} else if (NatureIndex > ReportedIndex) {
					reportStr = plainMsg.substring(ReportedIndex + 9, NatureIndex);
				}
				reportStr = reportStr.trim();
			}
			
			if (CommentIndex > 0) {
				commentStr = plainMsg.substring(CommentIndex + 9);
				commentStr = commentStr.trim();
			}
			
			parsedMessage.Code = nature;
			parsedMessage.text = nature + " " + addressStr + " " + reportStr + " " + commentStr;
			parsedMessage.Address = addressStr;

			if (get_latlon(plainMsg, parsedMessage, msgService) == 0) {
			    parsedMessage.ErrorFlag = 0;
			}
			else {
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
			}
		} catch (Exception ex) {
			parsedMessage.ErrorFlag = 1;
		}
		return parsedMessage;
	}
	
    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
	var p = Pattern.compile(".*Lat= (-?\\d+\\.\\d+) Lon= (-?\\d+\\.\\d+) .*");
	var m = p.matcher(msg);
	var lat = "";
	var lon = "";

	if (m.matches()) {
	    lat = m.group(1);
	    lon = m.group(2);

	    log.info("lat=" + lat);
	    log.info("lon=" + lon);

	    var dlat = Double.parseDouble(lat);
	    var dlon = Double.parseDouble(lon);
	    
	    var pos = new Position(dlon, dlat);
	    var point = new Point(pos);

	    pm.messageLatLon = point;

	    try {
		pm.location = ms.getLocationByPoint(pm.messageLatLon);
		
		log.info(pm.messageLatLon.toString());
		return 0;
	    }
	    catch (Exception e) {
		log.error("getLocationByPoint failed for " + lat + " " + lon, e);
	    }
	}

	return 1;
    }
}
