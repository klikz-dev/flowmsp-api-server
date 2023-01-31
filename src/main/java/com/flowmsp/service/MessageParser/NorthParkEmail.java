package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.google.common.base.Strings;
import com.mongodb.client.model.geojson.Point;

public class NorthParkEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				// Msg- STRUCTURE FIRE^18-05-1203 ^UNIT:891^5005 TAMERY LN, WINNEBAGO COUNTY
				plainMsg = plainMsg.replace("+", " ");
				plainMsg = plainMsg.replace("%3A", ":");
				plainMsg = plainMsg.replace("%2F", "/");
				plainMsg = plainMsg.replace("%2C", ",");
				plainMsg = plainMsg.replace("%2E", ".");
				plainMsg = plainMsg.replace("\r\n", " ");
				plainMsg = plainMsg.replace("\n", " ");
				plainMsg = msgService.removeHyperlink(plainMsg);
				
				//================== This is the start of signature
				int idx = plainMsg.indexOf("==================");
				if (idx > 0) {
					plainMsg = plainMsg.substring(0, idx).trim();
				}
				
				parsedMessage.text = plainMsg;
				parsedMessage.Code = "";
				
				idx = plainMsg.indexOf("Subject-");
				//Subject is of No Use
				int jdx = plainMsg.indexOf("Msg-");
				
				if (idx >= 0 && jdx > 0) {
					plainMsg = plainMsg.substring(jdx + 4);
				}				

				// Msg- STRUCTURE FIRE^18-05-1203 ^UNIT:891^5005 TAMERY LN, WINNEBAGO COUNTY
				// Address is 891 5005 TAERY LN, WINNEDBAGO COUNTY
				String[] msgComponent = plainMsg.split("\\^");
				parsedMessage.Code = msgComponent[0];
				for (int ii = 0; ii < msgComponent.length; ii++) {
					if (Strings.isNullOrEmpty(msgComponent[ii])) {
						continue;
					}
					if (msgComponent[ii].startsWith("UNIT:")) {
						//next must be the address
						if (ii + 1 < msgComponent.length) {
							plainMsg = msgComponent[ii + 1];
							break;
						}
					}
				}

				parsedMessage.Address = plainMsg;
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
}
