package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class CrestwoodEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//1/30/2019 12:21:30 AM 2390300273 13259 S CENTRAL AV MAP PAGE:22 SMELL OF GAS FIRST FLOOR KITCHEN  
				String plainMsg = msg;
				int idx = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
				
				//#1 - Search for " MAP "		
				String tmpStr = plainMsg;
				idx = tmpStr.indexOf(" MAP PAGE");
				if (idx > 0) {
					tmpStr = tmpStr.substring(0, idx).trim();			
				}
				
				idx = tmpStr.indexOf(" AM ");
				if (idx > 0) {
					tmpStr = tmpStr.substring(idx + 4);
				}
				
				idx = tmpStr.indexOf(" PM ");
				if (idx > 0) {
					tmpStr = tmpStr.substring(idx + 4);
				}
				String[] words = tmpStr.split("\\s");
				
				String addressStr = "";
				int kk = 0;
				for (int ii = 0; ii < words.length; ii ++) {
					if (words[ii].isEmpty()) continue;
					kk ++;
					if (kk <= 1) continue;
					addressStr = addressStr + words[ii] + " ";
				}
				addressStr = addressStr.trim();
				
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