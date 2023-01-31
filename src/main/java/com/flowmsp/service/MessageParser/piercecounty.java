package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class piercecounty implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(piercecounty.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("piercecounty msg");
        log.info(msg);

	/*
          <?xml version=3D"1.0" encoding=3D"utf-8"?>
          <CadCalls>
            <CadCall>
              <CadCallPrimkey>13b84f8b-d940-437b-a89b-d8390bc3898a</CadCallPrimkey>
              <CadCallNo>173358</CadCallNo>
              <CadCallCaseNo>2020-00173</CadCallCaseNo>
              <IncidentDate>04/29/2020 12:25:27</IncidentDate>
              <IncidentLocation />
              <IncidentAddNo>6226</IncidentAddNo>
              <IncidentStreet>PALMETTO WAY</IncidentStreet>
              <IncidentCity>BLACKSHEAR</IncidentCity>
              <PrimUnitNo>113D1</PrimUnitNo>
              <DispatchTime>04/29/2020 12:27:38</DispatchTime>
              <OnSceneTime>01/01/1800 00:00:00</OnSceneTime>
              <ClearTime>04/29/2020 12:32:32</ClearTime>
              <IncidentType>FIRE ALARM</IncidentType>
              <IncidentComment>LIVING ROOM GAS DETECTOR</IncidentComment>
              <IncidentCleared>04/29/2020 12:32:32</IncidentCleared>
              <IncidentLat>31.26266556</IncidentLat>
              <IncidentLon>-82.34441455</IncidentLon>
            </CadCall>
            <CadContact>
              <CadCallMulti>No</CadCallMulti>
              <CadCallContactPrimkey>79db0ea7-d054-4913-b636-c50a89adb667</CadCallContactPrimkey>
              <CadCallPrimkey>13b84f8b-d940-437b-a89b-d8390bc3898a</CadCallPrimkey>
              <CadCallNo>173358</CadCallNo>
              <FirstName>JOHN</FirstName>
              <LastName>HOPKINS</LastName>
              <Address>6226</Address>
              <Street>PALMETTO WAY</Street>
              <City>BLACKSHEAR</City>
              <Phone>(912)285-5511</Phone>
            </CadContact>
            <CadCallNotes>
              <NotePrimkey>9a0dd0b8-1b90-46fb-b0bc-808a5d9e791e</NotePrimkey>
              <Note1>REF# 1433165</Note1>
            </CadCallNotes>
            <CadCallNotes>
              <NotePrimkey>df2ee478-9935-4ec3-82b0-8ec39773ed16</NotePrimkey>
              <Note2>ALARM CO CALLED BACK AND REQ TO CANCEL, THEY MADE CONTACT WITH K=
          EYHOLDER AND VERIFIED FALSE ALAR,</Note2>
            </CadCallNotes>
          </CadCalls>
	 */
        try {
            String [] messageArray = msg.split("\n");

	    var CadCallCaseNo = get_element(messageArray, "CadCallCaseNo");
	    var IncidentDate = get_element(messageArray, "IncidentDate");
	    var IncidentLocation = get_element(messageArray, "IncidentLocation");
	    var IncidentAddNo = get_element(messageArray, "IncidentAddNo");
	    var IncidentStreet = get_element(messageArray, "IncidentStreet");
	    var IncidentCity = get_element(messageArray, "IncidentCity");
	    var IncidentType = get_element(messageArray, "IncidentType");
	    var IncidentComment = get_element(messageArray, "IncidentComment");
	    var IncidentLat = get_element(messageArray, "IncidentLat");
	    var IncidentLon = get_element(messageArray, "IncidentLon");

	    parsedMessage.Address = IncidentAddNo + " " + IncidentStreet + "," + IncidentCity ;
	    parsedMessage.Code = IncidentType ;

	    parsedMessage.text  = CadCallCaseNo + "\n" ;
	    parsedMessage.text += IncidentDate + "\n" ;
	    parsedMessage.text += IncidentLocation + "\n" ;
	    parsedMessage.text += IncidentAddNo + "\n" ;
	    parsedMessage.text += IncidentStreet + "\n" ;
	    parsedMessage.text += IncidentCity + "\n" ;
	    parsedMessage.text += IncidentType + "\n" ;
	    parsedMessage.text += IncidentComment + "\n" ;
	    parsedMessage.text += IncidentLat + "\n" ;
	    parsedMessage.text += IncidentLon + "\n" ;

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (get_latlon(IncidentLat, IncidentLon, parsedMessage, msgService) == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }

	    //Address should be of at-least 5 characters
	    else if (parsedMessage.Address.length() > 5) {
		parsedMessage.ErrorFlag = 0;
		
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
        } catch (Exception ex) {
	    //log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private int get_latlon(String IncidentLat, String IncidentLon, ParsedMessage pm, MessageService ms) {
	var lat = Double.parseDouble(IncidentLat);
	var lon = Double.parseDouble(IncidentLon);

	if (lat == 0.0 || lon == 0.0)
	    return 1;

	var pos = new Position(lon, lat);
	var point = new Point(pos);
	
	pm.messageLatLon = point;
	
	try {
	    pm.location = ms.getLocationByPoint(pm.messageLatLon);
	}
	catch (Exception e) {
	    log.error("getLocationByPoint failed for " + lat + " " + lon, e);
	}
	
	log.info(pm.messageLatLon.toString());

	return 0;
    }

    // eg. IncidentStreet
    private String get_element(String[] lines, String element) {
	var s = "<" + element + ">(.*)</" + element + ">";
	var p = Pattern.compile(s);

	log.info("pattern=" + s);

	for (int i = 0; i < lines.length; i++) {
	    var line = lines[i].trim();
	    var m = p.matcher(line);

	    if (m.matches()) {
		log.info(i + " found pattern " + m.group(1)) ;
		return m.group(1);
	    }
	}

	return "";
    }
}
