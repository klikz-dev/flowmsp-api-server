package com.flowmsp.service.MessageParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

public class LitchfieldEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
			/*Subject- Msg-911:[894]- NATURE: ALARMS LOCATION: 1403 STAMER DR LITCHFIELD BETWEEN MAP PAGE 34A COMMENTS: LITCHFIELD FIRE ADV RECIEVED CALL FROM ALARM COMPANY FOR FIRE ALARM / DROP TONES*/
				String plainMsg = msg;
				int idx = -1;
				int endIndex = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
			
				String incident = "";
				int IncidentIndex = plainMsg.indexOf(":");
				int LocationIndex = plainMsg.indexOf("LOCATION:");
				int BetweenIndex = plainMsg.indexOf("BETWEEN");
				int CommentsIndex = plainMsg.indexOf("COMMENTS:");
				
				if (IncidentIndex >= 0 && IncidentIndex < LocationIndex) {
					incident = extract_string(plainMsg, IncidentIndex + 1, LocationIndex - 1);
				}
				
				/*LOCATION: 1403 STAMER DR LITCHFIELD BETWEEN MAP PAGE 34A COMMENTS: LITCHFIELD FIRE ADV RECIEVED CALL FROM ALARM COMPANY FOR FIRE ALARM / DROP TONES*/
				String addressStr = "";
				if (LocationIndex >= 0) {
					if (BetweenIndex > LocationIndex) {
						addressStr = extract_string(plainMsg, LocationIndex + 10, BetweenIndex - 1);
					} else {
						//Comments
						if (CommentsIndex > LocationIndex) {
							addressStr = extract_string(plainMsg, LocationIndex + 10, CommentsIndex - 1);
						} else {
							addressStr = extract_string(plainMsg, LocationIndex + 10, plainMsg.length() - 1);
						}
					}
				}

				parsedMessage.Code = incident;
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
