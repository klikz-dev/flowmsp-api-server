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
05/16/21 22:39
601 3RD ST, CAMANCHE, IA 52730

41.783635
-90.251809
1300 - 1300; 1300 - 1313; 1300 - 1351; CaPD - 408
1313 - Enroute 05/16/21 22:49:43; 1313 - On Scene 05/16/21 22:50:33; 1313 - Complete 05/16/21 23:03:07; 1300 - Assign 05/16/21 22:40:53; 1300 - Available 05/16/21 23:03:01; 1351 - Enroute 05/16/21 22:47:25; 1351 - On Scene 05/16/21 22:48:44; 1351 - Available 05/16/21 23:03:01; 408 - On Scene 05/16/21 22:43:54; 408 - Available 05/16/21 22:57:23
Medical
UNCONSCIOUS / FAINTING - ALERT WITH ABNORMAL BREATHING
 <hr>
05/16/21 22:40:53
05/16/21 22:43:54 *
05/16/21 22:43:54
05/16/21 23:03:07
 */

public class clinton1926 implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(clinton1926.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        try {
            String[] lines = msg.split("\r?\n");

            get_address(parsedMessage, lines);
            var rc = get_latlon(lines, parsedMessage, msgService);
            get_units(parsedMessage, lines);
            parsedMessage.Code = getCode(lines);
            parsedMessage.text = get_text(lines);

            log.info("Address: " + parsedMessage.Address);
            log.info("Units: " + parsedMessage.units);

            //Address should be of at-least 5 characters
            if (rc == 1 && parsedMessage.Address.length() > 5) {
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
            // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private String get_text(String[] lines) {
        var text = "";

        for (var line : lines) {
            if (line.contains("<hr>"))
                break;

            text += (line + " ");
        }

        return text.trim();
    }

    private int get_latlon(String[] lines, ParsedMessage pm, MessageService ms) {

        var pLat = Pattern.compile("([+]?\\d+\\.\\d+)");
        var pLon = Pattern.compile("([-]?\\d+\\.\\d+)");
        var lat = "";
        var lon = "";

        for (var line : lines) {
            var mLat = pLat.matcher(line);
            var mLon = pLon.matcher(line);

            if (mLat.matches())
                lat = mLat.group(1);
            else if (mLon.matches())
                lon = mLon.group(1);
        }

        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat) ;
            var dlon = Double.parseDouble(lon) ;

            var pos = new Position(dlon, dlat);
            var point = new Point(pos);

            pm.messageLatLon = point;

            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.info(pm.messageLatLon.toString());
                return 0;
            }
            catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
        }

        return 1;
    }

    private void get_address(ParsedMessage pm, String[] lines) {
        if (lines.length > 2) {
            pm.Address = lines[1];
        }
        else
        {
            pm.Address = "";
        }

        return;
    }

    /* 1300 - 1300; 1300 - 1313; 1300 - 1351; CaPD - 408 */
    private void get_units(ParsedMessage pm, String[] lines) {
        for (var line : lines) {
            if (line.startsWith("1300 - ")) {
                for (var pair : line.split(";"))
                {
                    var fields = pair.split(" - ");
                    if (fields.length > 1)
                    {
                        pm.units.add(fields[1].trim());
                    }
                }
            }
        }

        return;
    }

    private String getCode(String[] lines)
    {
        for (int i = 0; i<lines.length; i++ )
        {
            if (lines[i].contains("<hr>"))
            {
                if (i > 2)
                {
                    return lines[i-2];
                }
            }
        }

      return "";
    }
}
