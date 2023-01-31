package com.flowmsp.service.MessageParser;

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

import java.util.regex.Pattern;

/*
Call:Cardiac Arrest 9B9 Place: Addr:300 Nostalgia Ln City:Zebulon ID:20-0145530 Pri:E0_High Date:10/29/2020Time:15:48:41Map:055H Unit:ZFE91,EMS64,DIST6,EMS60 Info:[1] Class of Service: wireless - Cell,[2] LAT: 35.829109 LON: -78.362541,[3] Multi-Agency LAW Incident #: 201061379,[4] CALLERS THINKS HIS MOM PASSED AWAY [Shared],[5] Automatic Case Number(s) issued for Incident #[20-0145530], Jurisdiction: Zebulon Fire Department. Case Number(s): 20-001399. requested by ZFE91. [Shared],[6] Automatic Case Number(s) issued for Incident #[20-0145530], Jurisdiction: Wake Co EMS System. Case Number(s): 20-089552. requested by EMS64. [Shared], TAC:TAC 13
 */

public class wake_county_nc_psap implements PSAPMsgParser {
    private static final Logger log = LoggerFactory.getLogger(wake_county_nc_psap.class);

    @Override
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("wake_county_nc_psap msg");
        log.info(msg);

        try {
            parsedMessage.psap = psap;
            parsedMessage.Code = get_code(msg);
            parsedMessage.text = get_text(msg);

            //TODO we will not have customer here
            //parsedMessage.Address = get_address(msg, cust);
            var has_latlon = get_latlon(msg, parsedMessage, msgService);


            get_units(msg, parsedMessage);

            log.info("Code: " + parsedMessage.Code);
            log.info("Address: " + parsedMessage.Address);
            log.info("Units: " + parsedMessage.units);

            if (has_latlon == 0) {
                log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
            } else {
                for (String unit : parsedMessage.units) {
                    Customer customer = psapService.getCustomerFromUnit(unit, psap.id);
                    if (customer != null) {
                        parsedMessage.Address = get_address(msg, customer);

                        //Address should be of at-least 5 characters
                        if (parsedMessage.Address.length() > 5) {
                            boolean isLocationRetrieved = getLocationByAddress(msgService, parsedMessage, customer);
                            if (isLocationRetrieved) break; //needed this so we can stop loking for location after we already found it in one unit
                        }
                    }
                }
            }
            parsedMessage.ErrorFlag = 0;
        } catch (Exception ex) {
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

    private String get_text(String msg) {
        return msg;
    }

    private String get_code(String msg) {
        var p = Pattern.compile("Call:(.*)Place:.*");
        var m = p.matcher(msg);

        if (m.matches())
            return m.group(1);
        else
            return "";
    }

    private String get_address(String msg, Customer cust) {
        var pAddr = Pattern.compile(".*Addr:(.*)City:.*");
        var ma = pAddr.matcher(msg);
        var addr = "";

        if (ma.matches()) {
            addr = ma.group(1).trim();

            addr = addr.replace(" / ", " & ");
        }

        var pCity = Pattern.compile(".*City:(.*)ID:.*");
        var mc = pCity.matcher(msg);

        if (mc.matches())
            addr += (", " + mc.group(1).trim());

        // append state
        if (cust.address.state.length() > 0)
            addr += (", " + cust.address.state);

        // append zip
        if (cust.address.zip.length() > 0)
            addr += ("  " + cust.address.zip);

        return addr;
    }

    private int get_units(String msg, ParsedMessage pm) {
        var p = Pattern.compile(".* Unit:([A-Z0-9,]+) .*");
        var m = p.matcher(msg);

        if (m.matches()) {
            var units = m.group(1).split(",");

            for (String u : units)
                pm.units.add(u);

            return 0;
        }

        return 1;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
        var pLAT = Pattern.compile(".*LAT: ?([-+]?\\d+\\.\\d+).*");
        var mLAT = pLAT.matcher(msg);
        var pLON = Pattern.compile(".*LON: ?([-+]?\\d+\\.\\d+).*");
        var mLON = pLON.matcher(msg);
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

            try {
                pm.location = ms.getLocationByPoint(pm.messageLatLon);

                log.info(pm.messageLatLon.toString());
                return 0;
            } catch (Exception e) {
                log.error("getLocationByPoint failed for " + lat + " " + lon, e);
            }
        }

        return 1;
    }

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        return null;
    }
}
