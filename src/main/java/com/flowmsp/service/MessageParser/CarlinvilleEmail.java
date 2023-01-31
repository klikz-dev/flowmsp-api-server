package com.flowmsp.service.MessageParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class CarlinvilleEmail implements MsgParser {
	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//DISPATCH:CA FD:CA FIRE - 09/28 06:37 - CA  FD:18-000130 0006 BREATHING PROBLEMS 611 HARRINGTON ST,CARLINVILLE  CA  FD:CA FIRE  57-year-old, Male, Conscious, Breathing. CC Text: Breathing Problems Problem: DIFF BREATHING
				/*
				 * DISPATCH:CA
					FD:CA FIRE - 09/26 13:00 - CA
					FD:18-000128 8167 AMBULANCE CALL
					
					111 E CHERRY ST,Apt/Unit #6, CARLINVILLE
					CA
					FD:CA FIRE
					62 YEAR OLD FEMALE CANCER PATIENT HAVING CHEST AND STOMACH PAINS NURSE ON SCENE
				 */
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
					//DISPATCH:CA FD:CA FIRE - 09/28 06:37 - CA  FD:18-000130 0006 BREATHING PROBLEMS 611 HARRINGTON ST,CARLINVILLE  CA  FD:CA FIRE  57-year-old, Male, Conscious, Breathing. CC Text: Breathing Problems Problem: DIFF BREATHING
					String [] msgArr = plainMsg.split("FD:");		
					Pattern p = Pattern.compile("-?\\d+");
					Matcher m = p.matcher(msgArr[2]);
					addressIndex = 0;
					while (m.find()) {
						addressIndex = msgArr[2].indexOf((m.group()));		  
					}
					addressIndexEnd = msgArr[2].indexOf("CARLINVILLE");
					if (addressIndexEnd > 0) {
						addressStr = msgArr[2].substring(addressIndex, addressIndexEnd + 11);
					} else {
						addressStr = msgArr[2].substring(addressIndex);
					}
					incident = msgArr[2].substring(0, addressIndex);
				} else {
					for(int ii = 0; ii < messageArray.length; ii++){
						if(messageArray[ii].isEmpty()){
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
						addressStr = messageArray[addressIndex];
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
