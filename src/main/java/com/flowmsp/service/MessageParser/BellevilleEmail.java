package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;

public class BellevilleEmail implements MsgParser {

	@Override
	public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
		ParsedMessage parsedMessage = new ParsedMessage();
		parsedMessage.ErrorFlag = 99;
		try {
				//Subject-External Service Request Msg-ActiveReports Document Belleville Police Department 720 W Main Street, Belleville, IL 62220 Event Report Event No: 2019-023004 Status: Enroute Disposition: Category: FIRE INVESTIGATION /  STILL Complaint Numbers Complaint Number: Unit: Reporting DSN: Agency: Address: 301 E MAIN ST, BELLEVILLE, IL Precinct: B 1 Sector: B 5 GEO: ESZ: BFD 4 Ward: WAR D 2 Intersection: N CHURCH ST Date / Time Open: 04/23/2019 13:22:45 Law Enf.: Belleville PD Dispatch: Fire: Belleville FD Enroute: 04/23/2019 13:24:06 EMS: MedStar Arrival: Source: TC28 Departure: Closed: Person(s) Involved Name Address Phone Business TAVERN ON MAIN 301 E MAIN ST, BELLEVILLE, IL 62220 (618) 233-6246  Vehicle(s) Model Plate Make Color Type VIN Incident Notes: **04/23/2019 13:23:58 TC28** CHECKING THE KITCHEN AT TAVERN ON MAIN Event Log Date / Time Activity Unit DSN Dispatcher Status 04/23/2019 13:23:58 Event open by sending work area TC28 OPENED 04/23/2019 13:24:06 ENROUTE TO SCENE 1211 TC28 ENROUTED Page 1 of 1 Printed 04/23/2019 1324 c 1994 - 2019 Omnigo Software St. Louis MO  omnigo.com
				String plainMsg = msg;
				String subject = "";
				int idx = -1;
				int jdx = -1;
				jdx = plainMsg.indexOf("Subject-");				
				idx = plainMsg.indexOf("Msg-");
				if (idx >= 0 && jdx >= 0) {
					subject = plainMsg.substring(jdx + 8, idx -1);
				}
				
				if (idx >= 0) {
					plainMsg = plainMsg.replace("Msg-", "");
					plainMsg = plainMsg.substring(idx);
					plainMsg = plainMsg.trim();
				}
				//Address: 301 E MAIN ST, BELLEVILLE, IL Precinct: B 1 
				String addressStr = "";
				int PrecinctIndex = plainMsg.indexOf("Precinct:");
				int LocationIndex = plainMsg.indexOf("Address:");
				if (LocationIndex >= 0 && LocationIndex < PrecinctIndex) {
					addressStr = extract_string(plainMsg, LocationIndex + 9, PrecinctIndex - 1);
					addressStr = addressStr.trim();
				}
				String incidentStr = "";
				//Incident Notes: **04/23/2019 13:23:58 TC28** CHECKING THE KITCHEN AT TAVERN ON MAIN Event Log
				int NotesIndex = plainMsg.indexOf("Incident Notes:");
				if (NotesIndex > 0) {
					int EventLogIndex = plainMsg.indexOf("Event Log", NotesIndex);
					if (EventLogIndex > 0) {
						incidentStr = extract_string(plainMsg, NotesIndex + 15, EventLogIndex - 1);
						incidentStr = incidentStr.replaceAll("\\*", " ").trim();						
					}
				}
				plainMsg = subject + ";\n" + addressStr + ";\n" + incidentStr;
				
				parsedMessage.Code = subject;
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
