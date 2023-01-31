package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

/*
3/27/2021 02:35:06   <Call Type> /  111 BECK RD, PENDERGRASS /7068700514
 / 2021-00002423 (0780202) /
MALE FELL IN THE BATHTUB AND SOUNDED IN PAIN// UNABLE TO GET AN ADDRESS//
attempted to call back no answer voicemail not set up
PER CALL HX 111 BECK ROAD// THOMAS SUBJ// ATTEMPTED CALL BACK AND NEG CONTACT
*/
public class northjackson implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(northjackson.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            msg = remove_subject(msg);
            msg = remove_leading_timestamp(msg);

            parsedMessage.text = get_message_text(msg);
            parsedMessage.Code = get_code(msg);
            parsedMessage.Address = get_address(cust, msg);
            parsedMessage.incidentID = get_incident(msg);

            if (parsedMessage.Address.length() > 5) {
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
            // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private String remove_subject(String msg) {
        var p = Pattern.compile("Subject-.* Msg-(.*)");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1);
        else
            return msg;
    }
    private String remove_leading_timestamp(String msg) {
        var p = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4} \\d{2}:\\d{2}:\\d{2}(.*)");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1);
        else
            return msg;
    }

    private String get_message_text(String msg) { return msg; }

    // 2021-00000160 (07809)
    private String get_incident(String msg) {
        var p = Pattern.compile(".* (\\d+-\\d+) .*");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1).trim();
        else
            return "";
    }

    private String get_code(String msg) {
        var flds = msg.split(" / ");

        if (flds.length > 1)
            return flds[1].trim();
        else
            return "";
    }

    private String get_address(Customer cust, String msg) {
        var flds = msg.split(" / ");
        var addr = "";

        if (msg.startsWith(" / ")) {
            if (flds.length > 2)
                addr = flds[2].trim();
        }
        else
            addr = flds[0].trim();

        // append state
        if (addr.length() > 0 && cust.address.state.length() > 0)
            addr += (", " + cust.address.state);

        return addr;
    }
}
