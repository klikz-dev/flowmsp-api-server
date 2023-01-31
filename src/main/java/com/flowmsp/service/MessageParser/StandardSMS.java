package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class StandardSMS implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				// FIRE+41+RIVERGATE+DR%2C+WILTON+CT+06897+TIME%3A12%3A34+DATE%3A12%2F03%2F2018
				// Convert + to Space
				plainMsg = plainMsg.replace("\\u0026lt;", "");
				plainMsg = plainMsg.replace("\\u0026;", "");
				plainMsg = plainMsg.replace("\\u0026gt;", "");
				plainMsg = plainMsg.replace("&lt;", "");
				plainMsg = plainMsg.replace("&gt;", "");
				plainMsg = plainMsg.replace("+", " ");
				plainMsg = plainMsg.replace("%3A", ":");
				plainMsg = plainMsg.replace("%2F", "/");
				plainMsg = plainMsg.replace("%2C", ",");
				plainMsg = plainMsg.replace("%2E", ".");
				plainMsg = plainMsg.replace("\r\n", " ");
				plainMsg = plainMsg.replace("\n", " ");
			
				parsedMessage.text = plainMsg;

				String[] msgTypes = new String[] { "AMBULANCE", "LIFT ASSIST", "FIRE:VEHICLE", "SMOKE/ODOR", "SMOKE", "ALARM:FIRE", "ALARM:STILL",
						"ACCIDENT", "FIRE:STRUCTURE", "FIRE:MUTAL AID", "FIRE", "CONTROL BURN", "ALARMCO:DET", "ALARMCO: DET", "ALARM:CO DET", "ALARM", "WIRES DOWN", "MISSING PERSON", "BREATHING PROBLEMS", "NEW", "NEW UNKNOWN" };
				for (String str : msgTypes) {
					String searchStr1 = str;
					String searchStr2 = "CALL " + str;
					String searchStr3 = str + " CALL";
					if (plainMsg.startsWith(searchStr1)) {
						parsedMessage.Code = str;
						plainMsg = plainMsg.replaceFirst(searchStr1, "");
						break;
					}
					if (plainMsg.startsWith(searchStr2)) {
						parsedMessage.Code = str;
						plainMsg = plainMsg.replaceFirst(searchStr2, "");
						break;
					}
					if (plainMsg.startsWith(searchStr3)) {
						parsedMessage.Code = str;
						plainMsg = plainMsg.replaceFirst(searchStr2, "");
						break;
					}
				}

				String[] msgSubType = new String[] {"INJURY", "BLOOD"};
				for (String str : msgSubType) {
					String searchStr1 = str;
					String searchStr2 = "WITH " + str;
					if (plainMsg.startsWith(searchStr1)) {
						plainMsg = plainMsg.replaceFirst(searchStr1, "");
						break;
					}
					if (plainMsg.startsWith(searchStr2)) {
						plainMsg = plainMsg.replaceFirst(searchStr2, "");
						break;
					}
				}
				
				//Barrigton SMS requires this
				int idx = plainMsg.lastIndexOf("Map:");
				if (idx > 0) {
					plainMsg = plainMsg.substring(0, idx);
					plainMsg = plainMsg.trim();
				} else {
					idx = plainMsg.lastIndexOf("Units:");
					if (idx > 0) {
						plainMsg = plainMsg.substring(0, idx);
						plainMsg = plainMsg.trim();
					}					
				}
				// A Sample Message is Below
				// FIRE:MUTAL AID 9841 W DRALLE RD Frankfort TIME: 08:49 DATE: 2/15/2018
				idx = plainMsg.lastIndexOf("TIME:");
				if (idx > 0) {
					plainMsg = plainMsg.substring(0, idx);
					plainMsg = plainMsg.trim();
				}				
				
				parsedMessage.Address = plainMsg;
				//Address should be of at-least 5 characters
				if (parsedMessage.Address.length() > 5) {
					// Now PlainMsg has Address Only
					Location location = msgService.getLocationByAddress(plainMsg);
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
}
