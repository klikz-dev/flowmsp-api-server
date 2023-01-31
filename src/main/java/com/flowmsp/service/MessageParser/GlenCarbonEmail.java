package com.flowmsp.service.MessageParser;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlenCarbonEmail implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(GlenCarbonEmail.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("Glen Carbon msg");
        log.info(msg);

        String plainMsg = msg;

        if (plainMsg.contains("Msg-")) {
            plainMsg = plainMsg.substring(plainMsg.indexOf("Msg-") + "Msg-".length());
            plainMsg = plainMsg.trim();
        }

           /*
<style type='text/css'> div { white-space: pre; font-family: 'Times New Roman'; font-size: 12px } table { white-space: pre; font-family: 'Times New Roman'; font-size: 12px }</style><html><body><div><div>Call received at 
</div><div>
</div><div>Fire Hazardous Material
</div><div>
</div><div>
</div><div>
</div><div>106 SCHOOL ST, Glen Carbon
</div><div>
</div><div>SUMMIT AV / WERNER AV, BIRGER AV
</div><div>
</div><div>CO ALARM SOUNDING
</div><div>
</div><div>[2019-00001132 MC313]
</div><div>
</div><table cellspacing='0' cellpadding='0' width='auto'; text-align='left';><tr><td style="padding-left: 20px;">MC313: Glen Carbon Fire Protection District</td></tr><tr><td style="padding-left: 40px;">Assigned Station: GLCFS2</td></tr><tr><td style="padding-left: 60px;">Unit: 1900</td></tr><tr><td style="padding-left: 80px;">	Dispatched: 9/6/2019 15:47:24</td></tr><tr><td style="padding-left: 60px;">Unit: 1912</td></tr><tr><td style="padding-left: 80px;">	Dispatched: 9/6/2019 15:47:23</td></tr><tr><td style="padding-left: 60px;">Unit: 1913</td></tr><tr><td style="padding-left: 80px;">	Dispatched: 9/6/2019 15:47:24</td></tr></table><div></div></div></body></html>

2019-09-07 03:17:06,109 [main] INFO  c.f.s.MessageParser.GlenCarbonEmail - 2 matches Medical Call
2019-09-07 03:17:06,109 [main] INFO  c.f.s.MessageParser.GlenCarbonEmail - 6 matches 200 S STATION RD 9226, Glen Carbon
2019-09-07 03:17:06,109 [main] INFO  c.f.s.MessageParser.GlenCarbonEmail - 8 matches TRAILS END DR / TRAIL RIDGE CT
2019-09-07 03:17:06,110 [main] INFO  c.f.s.MessageParser.GlenCarbonEmail - 10 matches 92 YOF C/B FELL GASH ON HER LEG BACK PAIN
2019-09-07 03:17:06,110 [main] INFO  c.f.s.MessageParser.GlenCarbonEmail - 12 matches [2019-00001130 MC313]


             */
        try {
            String [] messageArray = plainMsg.split("\n");
	    var p = Pattern.compile("</div><div>(.+)");
	    var matches = 0;
	    var cross_streets = "";

            parsedMessage.text = "";
	    parsedMessage.Address = "";

            for (int i = 0; i < messageArray.length; i++) {
		var m = p.matcher(messageArray[i]);

		if (m.matches()) {
		    matches++;
		    log.info(i + " matches " + m.group(1));

		    parsedMessage.text += m.group(1);
		    parsedMessage.text += "\n";

		    if (i == 2)
			parsedMessage.Code = m.group(1);
		    else if (i == 4 || i == 6)
			parsedMessage.Address = parsedMessage.Address + m.group(1) + " ";
		    else if (i == 8)
			cross_streets = m.group(1);
		}
            }

	    log.info("code=" + parsedMessage.Code);
	    log.info("addr=" + parsedMessage.Address);
	    log.info("xst=" + cross_streets);

	    if (matches == 0) {
                log.info("GlenCarbon, pattern match failed");
                parsedMessage.ErrorFlag = 1;
            }
            else {
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
            }
        } catch (Exception ex) {
            parsedMessage.ErrorFlag = 1;
        }
        return parsedMessage;
    }
}

