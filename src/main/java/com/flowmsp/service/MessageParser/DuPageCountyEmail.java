package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.google.common.base.Strings;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

public class DuPageCountyEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//900 W LIBERTY DR WH: @PD | WHF19000436 | 05/14/19  15:51:54 | FALL-17M  | BRIDGE ST & COMMERCE DR | FN |  | WHF038 | 41.86361899999999991 -88.12025400000000275 |, StagingArea: Staging 1
				String plainMsg = msg;
				int idx = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
			
				String addressStr = "";
				String[] arr = plainMsg.split("\\|");
				if (arr.length > 0) {
					addressStr = arr[0];			
					addressStr = addressStr.trim();
				}
				parsedMessage.Code = "";
				parsedMessage.text = plainMsg;
				parsedMessage.Address = addressStr;

				boolean geoCoordinatesAvailable = false;
				Double latMsg = 0.0, lonMsg = 0.0;
				
				if (arr.length > 8)
				{
					//get Lat & Lon
					String geoStr = "";
					geoStr = arr[8].trim();
					if (!Strings.isNullOrEmpty(geoStr)) {
						String[] geoStrArr = geoStr.split(" ");
						latMsg = Double.parseDouble(geoStrArr[0]);
						lonMsg = Double.parseDouble(geoStrArr[1]);
						geoCoordinatesAvailable = true;
					}
				}
				
				if (geoCoordinatesAvailable) {
					Point latLon = new Point(new Position(lonMsg, latMsg));
					Location location = null;
					if (latLon != null) {
						parsedMessage.messageLatLon = latLon;
						location = msgService.getLocationByPoint(latLon);
					}
					if (location != null) {
						parsedMessage.location = location;
					}
				} else {
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
