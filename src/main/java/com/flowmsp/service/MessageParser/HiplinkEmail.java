package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class HiplinkEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//LOFD FLO1 EMS Sick Person DISP 317 E MAIN ST,LOW 6761 3161 17 44 18 09/18/2018 - SANDERS A (ProQA Medical) Chief Complaint Sick Person (Specific Diagnosis) 60-year-old, Female, Conscious, Breathing. Caller St 18SU15228 Between: HALSTEAD ST & GRANT ST 0
				/*LDFD
				FLD
				EMS Stroke
				DISP
				15623 GRANT ST,CRO
				6761 5851
				10 14 02 08/23/2018 - BRAZIL CH (ProQA Medical) 
				Chief Complaint  Stroke (CVA) / Transient Ischemic Attack (TIA) 
				79-year-old, Male, Conscious, Breathing. 
				C
				18LDF0238
				Between: W 159TH AV & W 153RD AV
				0*/
				String plainMsg = msg;
				int idx = -1;
				int endIndex = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
			
				String [] messageArray = plainMsg.split("\r\n");
				boolean singleLineMode = false;
				if (messageArray.length < 2) {
					singleLineMode = true;
				}
				int dispIndex = -1;
				int addressIndex = -1;
				int addressIndexEnd = -1;
				int incidentIndex = -1;
				String incident = "";
				String addressStr = "";

				if (singleLineMode) {
					//LOFD FLO1 EMS Sick Person DISP 317 E MAIN ST,LOW 6761 3161 17 44 18 09/18/2018 - SANDERS A (ProQA Medical) Chief Complaint Sick Person (Specific Diagnosis) 60-year-old, Female, Conscious, Breathing. Caller St 18SU15228 Between: HALSTEAD ST & GRANT ST 0
					dispIndex = plainMsg.indexOf("DISP ");
					if (dispIndex > 0) {
						addressIndex = dispIndex + 5;
						addressIndexEnd = plainMsg.substring(addressIndex).indexOf(",");
						if (addressIndexEnd > 0) {
							addressIndexEnd = addressIndex + addressIndexEnd;
							addressStr = plainMsg.substring(addressIndex, addressIndexEnd);
							String cityStr = plainMsg.substring(addressIndexEnd + 1);
							if (cityStr.startsWith("LOW")) {
								if (!addressStr.toLowerCase().contains("lowell")) {
									addressStr = addressStr + " Lowell";
								}
							} else if (cityStr.startsWith("STJ")) {
								if (!addressStr.toLowerCase().contains("st john")) {
									addressStr = addressStr + " St John";
								}
							} else if (cityStr.startsWith("HEB")) {
								if (!addressStr.toLowerCase().contains("hebron")) {
									addressStr = addressStr + " Hebron";
								}
							} else if (cityStr.startsWith("CRO")) {
								if (!addressStr.toLowerCase().contains("crown point")) {
									addressStr = addressStr + " Crown Point";
								}
							} else if (cityStr.startsWith("MUN")) {
								if (!addressStr.toLowerCase().contains("munster")) {
									addressStr = addressStr + " Munster";
								}
							}
						} else {
							addressStr = plainMsg.substring(addressIndex);
						}
						incident = plainMsg.substring(0, dispIndex - 1);						
					}
				} else {
					for(int ii = 0; ii < messageArray.length; ii++){
						if(messageArray[ii].equals("DISP")){
							dispIndex = ii;
							addressIndex = ii + 1;
							incidentIndex = ii - 1;
							break;
						}
					}
					if (dispIndex < 0) {
						//Not Found, Look for Pattern of Address
						for(int ii = 0; ii < messageArray.length; ii++){
							if(Pattern.matches("\\d+[a-zA-Z0-9,/-[\\s+]]+", messageArray[ii])){
								addressIndex = ii;
								incidentIndex = ii - 1;							
								break;
							}
						}
					}
					
					if (incidentIndex >= 0) {
						incident = messageArray[incidentIndex];
					}
					if (addressIndex >= 0) {
						String parsedAddress[] = messageArray[addressIndex].split(",");
						addressStr = parsedAddress[0];	
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
