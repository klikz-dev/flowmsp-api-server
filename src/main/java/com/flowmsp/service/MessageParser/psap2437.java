package com.flowmsp.service.MessageParser;

import com.flowmsp.SlugContext;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.psap.PSAPService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.HashMap;


/*
Message Format:
0 FYI: ;
1 (street address) ;
2 (call nature) ;
3  notes and timestamp of notes ;
4  lat ;
5  lon ;
6  responding agency ;
7  city abbreviation ;
8  units responding
 */

/*
FYI: ;7418 W 114TH PL;AMBULANCE/ASSIST CITIZEN;NEG SCREENING  [02/22/21 20:=
33:13 THUMBLE]
SUBJ UNABLE TO WALK  [02/22/21 20:33:01 THUMBLE]
;41.683620452;-87.80294799;NPFD;WORT;
 */
public class psap2437 implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(psap2437.class);
    private final HashMap<String, String> city_mapping = new HashMap<String, String>();

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("psap2437 msg");

        city_mapping.put("ALSI", "Alsip");
        city_mapping.put("ARG", "Argonne Lab");
        city_mapping.put("BED", "Bedford Park");
        city_mapping.put("BI", "Blue Island");
        city_mapping.put("BR", "Burr Ridge");
        city_mapping.put("BRID", "Bridgeview");
        city_mapping.put("CHGO", "Chicago");
        city_mapping.put("COOK", "Cook County Sheriff");
        city_mapping.put("CRES", "Crestwood");
        city_mapping.put("DAR", "Darien");
        city_mapping.put("DPG", "Dupage");
        city_mapping.put("HG", "Homer Glen");
        city_mapping.put("HGTS", "Palos Heights");
        city_mapping.put("HICK", "Hickory Hills PD");
        city_mapping.put("HILL", "Palos Hills");
        city_mapping.put("HODG", "Hodgkins");
        city_mapping.put("ISP", "Illinois State Police");
        city_mapping.put("JUS", "Justice");
        city_mapping.put("LEMO", "Lemont");
        city_mapping.put("LG", "Lagrange");
        city_mapping.put("LOCK", "Lockport");
        city_mapping.put("MCCO", "McCook");
        city_mapping.put("MERP", "Merrionette Park");
        city_mapping.put("MIDL", "Midlothian");
        city_mapping.put("MVCC", "Moraine Valley College");
        city_mapping.put("OAK", "Oak Lawn");
        city_mapping.put("OF", "Oak Forest");
        city_mapping.put("ORLA", "Orland Park");
        city_mapping.put("PARK", "Palos Park");
        city_mapping.put("POS", "Posen");
        city_mapping.put("ROBB", "Robbins");
        city_mapping.put("ROME", "Romeoville");
        city_mapping.put("SUM", "Summit");
        city_mapping.put("TP", "Tinley Park");
        city_mapping.put("TRIS", "Tristate FPD");
        city_mapping.put("WILL", "Will County");
        city_mapping.put("WLOS", "Willow Springs");
        city_mapping.put("WOOD", "Woodridge");
        city_mapping.put("WORT", "Worth");
        city_mapping.put("WS", "Western Springs");

        try {
            String[] flds = msg.trim().split(";");

            parsedMessage.Code = get_code(flds);
            parsedMessage.text = get_text(flds);

            get_units(parsedMessage, flds);

            var latlon = get_latlon(flds, parsedMessage, msgService);

            //Address should be of at-least 5 characters
            for (String unit : parsedMessage.units) {
                Customer customer = psapService.getCustomerFromUnit(unit, psap.id);

                if (customer != null) {
                    parsedMessage.Address = get_address(flds, customer);

                    if (latlon == 0)
                        break;

                    //Address should be of at-least 5 characters
                    if (parsedMessage.Address.length() > 5) {
                        SlugContext.setSlug(customer.slug);
                        boolean isLocationRetrieved = getLocationByAddress(msgService, parsedMessage, customer);
                        SlugContext.clearSlug();

                        //needed this so we can stop loking for location after we already found it in one unit
                        if (isLocationRetrieved)
                            break;
                    }
                }
            }

            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
            // log.error(ex);
            parsedMessage.ErrorFlag = 1;
        }

        return parsedMessage;
    }

    private boolean getLocationByAddress(MessageService msgService, ParsedMessage parsedMessage, Customer cust) {
        boolean isLocationRetrieved = false;
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
            isLocationRetrieved = true;
        }
        if (latLon != null) {
            parsedMessage.messageLatLon = latLon;
            isLocationRetrieved = true;
        }

        return isLocationRetrieved;
    }

    private String get_text(String[] lines) {
        return String.join(" ", lines);
    }

    // first one is the station id
    private void get_units(ParsedMessage pm, String[] lines) {
        if (lines.length <=  6)
            return;

        // at this point we at least have a responding agency
        var units = lines[6].trim();

        if (lines.length > 8 && lines[8].trim().length() > 0)
            units = lines[8].trim();

        // no information
        if (units.length() == 0)
            return;

        for (var u : units.split(","))
            pm.units.add(u.trim());

        return;
    }

    private String get_code(String[] lines) {
        if (lines.length >= 3)
            return lines[2];
        else
            return "";
    }

    private String get_address(String[] flds, Customer cust) {
        if (flds.length < 9)
            return "";

        var addr = flds[1];
        var city_code = flds[7];
        var city = "";

        if (city_mapping.containsKey(city_code)) {
            city = city_mapping.get(city_code);
            addr += (", " + city);
        }

        // add state from customer record
        addr += (", " + cust.address.state);

        return addr;
    }

    private int get_latlon(String[] flds, ParsedMessage pm, MessageService ms) {
        var p = Pattern.compile("([-+]?\\d+\\.\\d+).*");
        var mLAT = p.matcher(flds[4]);
        var mLON = p.matcher(flds[5]);
        var lat = "";
        var lon = "";

        if (mLAT.matches())
            lat = mLAT.group(1);

        if (mLON.matches())
            lon = mLON.group(1);

        log.info("lat=" + lat);
        log.info("lon=" + lon);

        if (lat.length() > 0 && lon.length() > 0) {
            var dlat = Double.parseDouble(lat);
            var dlon = Double.parseDouble(lon);

            var pos = new Position(dlon, dlat);
            var point = new Point(pos);

            pm.messageLatLon = point;

            /* commented this out as it requires a slug which we don't have
            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.info(pm.messageLatLon.toString());
                return 0;
            } catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
             */
        }

        return 1;
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }

}
