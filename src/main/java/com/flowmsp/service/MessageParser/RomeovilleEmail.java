package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class RomeovilleEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
			/*PremierOne Notification

			Status : Open
			
			Inc # : 2118062000001484
			Inc Type : AMPSYCH
			Inc Desc : AM-PSYCHIATRIC PROBLEMS
			
			Location : 1050 W ROMEO RD
			Loc Name : ROMEOVILLE PD
			Cross Strs : N FRIEH DR / GRAND BLVD
			Comments:	
			7:23:27 PM 6/20/2018*/
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
				int IncidentIndex = plainMsg.indexOf("Inc Desc :");
				if (IncidentIndex >= 0) {
					endIndex = plainMsg.indexOf("Location :", IncidentIndex);
					if (endIndex > 0) {
						incident = extract_string(plainMsg, IncidentIndex + 11, endIndex - 1);
					}
				}
				
				
				String addressStr = "";
				int LocationIndex = plainMsg.indexOf("Location :");
				if (LocationIndex >= 0) {
					endIndex = plainMsg.indexOf("Loc Name", LocationIndex);
					if (endIndex > 0) {
						addressStr = extract_string(plainMsg, LocationIndex + 10, endIndex - 1);
					} else {
						//Cross Strs might be given
						endIndex = plainMsg.indexOf("Cross Strs :", LocationIndex);
						if (endIndex > 0) {
							addressStr = extract_string(plainMsg, LocationIndex + 10, endIndex - 1);
						} else {
							//Comments might be given
							endIndex = plainMsg.indexOf("Comments", LocationIndex);
							if (endIndex > 0) {
								addressStr = extract_string(plainMsg, LocationIndex + 10, endIndex - 1);
							}	
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
