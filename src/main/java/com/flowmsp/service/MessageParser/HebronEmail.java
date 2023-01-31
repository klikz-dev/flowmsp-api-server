package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class HebronEmail implements MsgParser {
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//DISPATCH:F16, GARAGE/STORAGE FIRE, 542 W 650 S, BNT, btwn S 500 W and S 600 W, HF, 
				//HB3, mappage,XXXX, BARN IS ON FIRE, Units:1342, 3544, 742, HF, 
				//LO6642 - From CADSRV 11/04/2018 12:50:15
			
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
				
				parsedMessage.text = plainMsg;
				parsedMessage.Code = "";
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
				//DISPATCH:F16, GARAGE/STORAGE FIRE, 542 W 650 S, BNT, btwn S 500 W and S 600 W, HF,
				String[] msgPart = plainMsg.split(",");
				if (msgPart.length > 2) {
					parsedMessage.Code = msgPart[1].trim();
				}
				if (msgPart.length > 3) {
					parsedMessage.Address = msgPart[2].trim();					
				}
				if (msgPart.length > 4) {
					//This might be the part of address
					String city = msgPart[3].trim();
					if (city.startsWith("HEB")) {
						parsedMessage.Address += " Hebron";
					}
				}
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
	
	public String extract_string(String str, int startIndex, int endIndex) {
		String retStr = "";
		try {
			if (endIndex < startIndex) {
				endIndex = startIndex;
			}
			retStr = str.substring(startIndex, endIndex);
			retStr = retStr.trim();
		} catch (Exception ex) {
			retStr = "";
		}
		return retStr;
	}
}
