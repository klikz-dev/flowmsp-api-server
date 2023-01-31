package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class BolesEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				int idx = -1;
				//10236857<\ID> 2019-00025263<\IncidentNumber> 04/11/2019<\DispatchDate> 13:22:53<\DispatchTime> Z FALL 2<\Category> <\Subcategory> <\EstablishmentName> 955 LONGHORN LN, PACIFIC, MO 63069<\AddressLocation> BOF19-MVE19<\ESZ> <\SecondaryLocation> 038°28'15.5"<\Latitude> -090°46'55.4"<\Longitude> <\GENERAL> Scanned by YHTI SPAM firewall
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
				//Some trailer messages UNWANTED are coming after it
				idx = plainMsg.indexOf("<\\GENERAL>");
				if (idx > 0) {
					plainMsg = plainMsg.substring(0, idx);
					plainMsg = plainMsg.trim();
				}
				//#1 - Search for " <\\EstablishmentName> "		
				String tmpStr = plainMsg;
				String searchStr = "<\\EstablishmentName>";
				idx = tmpStr.indexOf(searchStr);
				if (idx > 0) {
					tmpStr = tmpStr.substring(idx + searchStr.length()).trim();			
				}
				
				//#2 - Search for "<\\AddressLocation> "
				searchStr = "<\\AddressLocation>";
				idx = tmpStr.indexOf(searchStr);
				if (idx > 0) {
					tmpStr = tmpStr.substring(0, idx).trim();
				}
				
				String addressStr = tmpStr.trim();
				
				//String seems to be in very unreadable form.
				tmpStr = plainMsg;
				//After every tag put a semicolon and next line character. 
				//Do this until everyone gets corrected.				
				for (int jj = 0; jj < 20; jj ++) {
					//Do this for MAX 20 tags. I don't want to loop endlessly.
					searchStr = "<\\";
					idx = tmpStr.indexOf(searchStr);
					if (idx > 0) {
						int jdx = tmpStr.indexOf(">", idx);
						if (jdx > 0) {
							searchStr = tmpStr.substring(idx, jdx + 1);
							tmpStr = tmpStr.replace(searchStr, ";");
							tmpStr = tmpStr.replace("; ;", ";");
						}
					} else {
						break;
					}
				}
				tmpStr = tmpStr.replace(";", ";\n");
				plainMsg = tmpStr;
				
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