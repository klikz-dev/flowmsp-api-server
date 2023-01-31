package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class WestmontEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//Subject-Unit M183 Msg-Unit: M183-Address: 1526 CRAB TREE DR-Info: 90 Y F SEIZURE-Disp at: 12/7/2018 10:43:57 AM-CAD Call: FWM181207004031WM120718
				String plainMsg = msg;
				int idx = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
			
				String addressStr = "";
				int IncidentIndex = plainMsg.indexOf("-Info:");
				int LocationIndex = plainMsg.indexOf("-Address:");
				if (LocationIndex >= 0 && LocationIndex < IncidentIndex) {
					addressStr = extract_string(plainMsg, LocationIndex + 10, IncidentIndex - 1);
				}
				parsedMessage.Code = "";
				parsedMessage.text = plainMsg;
				parsedMessage.Address = addressStr;
				
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
	
	public boolean IsZero(double d) {
		if (d > -0.001 &&  d < 0.001) {
			//This is fairly near to zero
			return true;
		}
		return false;
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
