package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.google.common.base.Strings;
import com.mongodb.client.model.geojson.Point;

public class OakLawnEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				//CAD:FYI: ;OLFD;NATURAL GAS LEAK INSIDE;KELLY NISSAN;4300 W 95TH ST;OAK LAWN;OL3;S11;2018152798;1805572;EN01,SQD1,BAT1,EN05
				plainMsg = plainMsg.replace("+", " ");
				plainMsg = plainMsg.replace("%3A", ":");
				plainMsg = plainMsg.replace("%2F", "/");
				plainMsg = plainMsg.replace("%2C", ",");
				plainMsg = plainMsg.replace("%2E", ".");
				plainMsg = plainMsg.replace("\r\n", " ");
				plainMsg = plainMsg.replace("\n", " ");
				plainMsg = msgService.removeHyperlink(plainMsg);
				
				int idx = plainMsg.indexOf("Subject-");
				//Subject is of No Use
				int jdx = plainMsg.indexOf("Msg-");				
				if (idx >= 0 && jdx > 0) {
					plainMsg = plainMsg.substring(jdx + 4);
				}

				parsedMessage.text = plainMsg;				
				// 0       ; 1  ; 2                     ; 3          ; 4            ;5       ;6  ;7  ; 8
				//CAD:FYI: ;OLFD;NATURAL GAS LEAK INSIDE;KELLY NISSAN;4300 W 95TH ST;OAK LAWN;OL3;S11;2018152798;1805572;EN01,SQD1,BAT1,EN05
				String[] parts = plainMsg.split(";");
				if (parts.length > 5) {
					parsedMessage.Code = parts[2];
					parsedMessage.Address = parts[4];
					if (Strings.isNullOrEmpty(parts[5])) {
						parsedMessage.Address = parts[4].trim() + " " + parts[5].trim();
					}					
				} else {
					//I'm sure that the message is not in the format
					parsedMessage.Code = "";
					parsedMessage.Address = plainMsg;
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
}
