package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class TriStateEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//INC01 1.0 EV-XXX 0       FTS18122100520212 KINGERY QUARTER            TS123IPSYCHIATRIC                        M PSYCH EVAL IN PARKING LOT   IVY LN
				//INC01 1.0 EV-XXX 0 FTS1901030000207816 MAYFAIR LN TS122EODOR INVESTIGATION SMELL OF GAS IN ENTIRE HOUSE SOMERSET LN
				String plainMsg = msg;
				int idx = -1;
				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
				
				//1. Find FTS= Tri state, FAF= addison, FBE=bensenville, FIF= Itasca, FWF= wood dale, FEF=Elmhurst
				
				//Now search for FTS, FAF, FBE, FIF, FWF, FEF
				//This Text must be in first 30 Characters of Message
				//This must be followed by 2 DIGITS
				String tmpStr = plainMsg;
				String[] keyWords = {"FTS", "FAF", "FBE", "FIF", "FWF", "FEF"};
				for (int jj = 0; jj < keyWords.length; jj ++) {
					idx = tmpStr.indexOf(keyWords[jj]);
					if (idx >= 0 && idx <= 30) {
						try {
							//Get Next Two Characters
							String isDigit = tmpStr.substring(idx + 3, idx + 3 + 2);
							int intIsDigit = Integer.parseInt(isDigit);
							//Found The P
							//INC01 1.0 EV-XXX 0       
							//FTS
							//190329
							//001300
							//7050 MADISON ST
							//TS122GFALLS                              
							//50 Y F FALL OUTSIDE           
							//HIGH GROVE BL
							tmpStr = tmpStr.substring(idx + 3 + 12);
							break;
						} catch (Exception numEx) {
							
						}						
					}
				}

				//tmpstr can't go more than MUTUAL AID
				idx = tmpStr.toUpperCase().indexOf("MUTUAL AID");
				if (idx > 3) {
					tmpStr = tmpStr.substring(0, idx - 3);
				}
				String[] words = tmpStr.split("\\s");
				String addressStr = "";			
				for (int ii = 0; ii < words.length; ii ++) {
					if (words[ii].isEmpty()) {
						continue;
					}
					//Search for a word which has numeric in it.
					String onlyAlphabets = words[ii].replaceAll("[^a-zA-Z]", "");
					String onlyNumeric = words[ii].replaceAll("[^0-9]", "");
					if (ii >= 2) {
						if (!onlyAlphabets.isEmpty() && !onlyNumeric.isEmpty()) {
							break;
						}
					}
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
