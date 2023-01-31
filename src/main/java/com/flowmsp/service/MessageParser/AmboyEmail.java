package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.google.common.base.Strings;
import com.mongodb.client.model.geojson.Point;

public class AmboyEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				//1447 NAUMAN RD Line5=AMBOY Line11=MEDICAL Line12=LIFELINE-JAMES Line13=888-289-2018 05/24/2018 16:04:56 73 YOA F FELL LIFT ASSIST NO INJURIES GARAGE DOOR KEY PAD 5874 DO HAVE DOG CASE
				//15 N BLACKSTONE AVE    Line5=AMBOY          Line11=MEDICAL  Line12=ADRIENNE  Line13=815-677-3441           05/24/2018 20:38:16     27 YOA FEMALE WITH CHEST PAINS
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

				// 1447 NAUMAN RD Line5=AMBOY Line11=MEDICAL Line12=LIFELINE-JAMES Line13=888-289-2018 05/24/2018 16:04:56 73 YOA F FELL LIFT ASSIST NO INJURIES GARAGE DOOR KEY PAD 5874 DO HAVE DOG CASE
				// Address is 1447 NAUMAN RD
				parsedMessage.Code = "";				
				idx = plainMsg.indexOf("Line11=");
				if (idx > 0){
					String code = plainMsg.substring(idx + 7); 
					jdx = code.indexOf("Line12=");
					if (jdx > 0){
						code = plainMsg.substring(0, jdx);
					}
					parsedMessage.Code = code;
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
