package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class motorola_premier_one implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				String plainMsg = msg;
				int idx = -1;
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
			
				//First read date time
				String dateTimeStr = "";
				int dateTimeIndex = plainMsg.indexOf("---");
				int endIndex = 0;
				if (dateTimeIndex == 0) {
					//It is starting from this '----' , let's delete these trailing
					endIndex = dateTimeIndex + 1;
					for (int ii = endIndex; ii < plainMsg.length(); ii ++) {
						if (!plainMsg.substring(ii, ii + 1).contentEquals("-")) {
							endIndex = ii;
							break;
						}
					}
					plainMsg = extract_string(plainMsg, dateTimeIndex, endIndex - 1);
					dateTimeIndex = plainMsg.indexOf("---");
				} 
				
				if (dateTimeIndex > 0) {
					dateTimeStr = extract_string(plainMsg, 0, dateTimeIndex - 1);
				}
				
				String incident = "";
				int IncidentIndex = plainMsg.indexOf("INCIDENT:");
				if (IncidentIndex >= 0) {
					IncidentIndex = plainMsg.indexOf("Descr:", IncidentIndex);
					if (IncidentIndex > 0) {
						endIndex = plainMsg.indexOf("Mod Circum:", IncidentIndex);
						if (endIndex > 0) {
							incident = extract_string(plainMsg, IncidentIndex + 7, endIndex - 1);
						}
					}
				}
				
				
				String addressStr = "";				
				int LocationIndex = plainMsg.indexOf("INCIDENT DETAILS LOCATION:");
				if (LocationIndex >= 0) {
					plainMsg =  plainMsg.replace("INCIDENT DETAILS LOCATION:", "");
				}
				LocationIndex = plainMsg.indexOf("Location:");
				if (LocationIndex >= 0) {
					endIndex = plainMsg.indexOf("Loc Name:", LocationIndex);
					if (endIndex > 0) {
						addressStr = extract_string(plainMsg, LocationIndex + 10, endIndex - 1);
					}
					int cityIndex = plainMsg.indexOf("City:", LocationIndex);
					if (cityIndex > 0) {						
						endIndex = plainMsg.indexOf("Building:", cityIndex);
						if (endIndex > 0) {
							String CITY = extract_string(plainMsg, cityIndex + 6, endIndex - 1);
							if (CITY.equalsIgnoreCase("JV")) {
								CITY = "Janesville";
								addressStr = addressStr + " " + CITY;
							} else if(CITY.equalsIgnoreCase("BE")) {
								CITY = "Beloit";
								addressStr = addressStr + " " + CITY;
							}
						}						
					}
				}
				
				String comments = "";
				int CommentIndex = plainMsg.indexOf("COMMENTS:");
				if (CommentIndex > 0) {
					endIndex = plainMsg.indexOf("---", CommentIndex);
					if (endIndex > 0) {
						comments = extract_string(plainMsg, CommentIndex + 10, endIndex - 1);
					}
				}				

				//12:30:54 PM 6/20/2018 -------------------------------------------------- ___ ___ ___ ___
				//INCIDENT DETAILS LOCATION: Location: 4200 W HY 14 Loc Name: Loc Descr: City: JT Building: Subdivision: Floor: Apt/Unit: Zip Code: 
				//Cross Strs: 1700 N BURDICK RD / 4000 N RIVERS EDGE DR Area: JF5 Sector: 5 Beat: JF43 Map Book: -------------------------------------------------- 
				//INCIDENT: Inc #: 00005191 Inc #: JVFD18062000005191 Priority: 1 Inc Type: Descr: ILL SUBJECT Mod Circum: Created: 12:30:47 PM 6/20/2018 Caller: Phone: (608) 774-9976 
				//-------------------------------------------------- SECONDARY RESPONSE LOCATION: -------------------------------------------------- 
				//UNITS DISPATCHED: JVFD/AM845 -------------------------------------------------- 
				//PERSONNEL DISPATCHED: JVFD/AM845 -------------------------------------------------- 
				//COMMENTS: MALE WITH BLACK DIARRHEA 80 YOA -------------------------------------------------- 
				//PREMISE HAZARD: The information contained in this message and in any attachment is intended only for the recipient. It may be privileged and confidential, and should be protected from disclosure. If you are not the intended recipient, or you have received this communication in error, please immediately notify the sender by replying to the message and delete it from your computer. Please be aware that any dissemination or copying of this communication is strictly prohibited. __--__

				parsedMessage.Code = incident;
				parsedMessage.text = incident + " " + addressStr + " " + dateTimeStr + " " + comments;				
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
