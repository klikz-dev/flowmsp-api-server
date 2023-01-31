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
TODO move table to a database table so it can be easily updated

<4748> Zone:C001 RADIO TONE ACTIVATION> Name:D 3 STATION RADIO
*/
public class dartmouthfir implements MsgParser {
    private static final Logger log = LoggerFactory.getLogger(dartmouthfir.class);

    @Override
    public ParsedMessage Process(Customer cust, String msg, MessageService msgService) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.ErrorFlag = 99;

        log.info("dartmouthfir msg");
        log.info(msg);

        try {
	    var idx = msg.indexOf("Msg-");
	    var plainMsg = "";

	    if (idx >= 0)
		plainMsg = msg.substring(idx + 4);
	    else
		plainMsg = msg;

            parsedMessage.text = plainMsg;

	    get_code(plainMsg, parsedMessage);
	    get_address(plainMsg, parsedMessage);
	    var latlon_ok = get_latlon(plainMsg, parsedMessage, msgService);

	    log.info("Code: " + parsedMessage.Code);
	    log.info("Address: " + parsedMessage.Address);

	    if (latlon_ok == 0) {
		log.info("using LAT/LON");
                parsedMessage.ErrorFlag = 0;
	    }
            else if (parsedMessage.Address.length() > 5) {
		log.info("using Address");
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

    private String get_acct(String msg) {
	var p = Pattern.compile("<(\\d+)>.*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("acct=" + m.group(1));
	    return m.group(1);
	}
	else
	    return "";
    }

    private int get_code(String msg, ParsedMessage pm) {
	var p = Pattern.compile("<\\d+> Zone:C\\d+ (.*)> Name:.*");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("code=" + m.group(1));
	    pm.Code = m.group(1);
	}

	return 0;
    }

    private int get_latlon(String msg, ParsedMessage pm, MessageService ms) {
	var acct = get_acct(msg);
	var lat = 0.0;
	var lon = 0.0;

        if (acct.equals("0000")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0001")) { lat = 41.6411936 ; lon = -71.04896430000001 ; }
        else if (acct.equals("0002")) { lat = 41.6366644 ; lon = -70.9641528 ; }
        else if (acct.equals("0004")) { lat = 41.6678182 ; lon = -70.9919922 ; }
        else if (acct.equals("0005")) { lat = 41.666967 ; lon = -70.9869076 ; }
        else if (acct.equals("0009")) { lat = 41.662048 ; lon = -70.986464 ; }
        else if (acct.equals("0010")) { lat = 41.7210842 ; lon = -70.9738723 ; }
        else if (acct.equals("0012")) { lat = 41.6425573 ; lon = -70.9854565 ; }
        else if (acct.equals("0015")) { lat = 41.6405502 ; lon = -71.0223829 ; }
        else if (acct.equals("0016")) { lat = 41.6384215 ; lon = -70.9896949 ; }
        else if (acct.equals("0017")) { lat = 41.6536264 ; lon = -70.9868346 ; }
        else if (acct.equals("0018")) { lat = 41.6878573 ; lon = -70.98997229999999 ; }
        else if (acct.equals("0019")) { lat = 41.6672168 ; lon = -70.98485409999999 ; }
        else if (acct.equals("0020")) { lat = 41.668916 ; lon = -70.99228269999999 ; }
        else if (acct.equals("0023")) { lat = 41.663811 ; lon = -70.9870237 ; }
        else if (acct.equals("0024")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0025")) { lat = 41.663811 ; lon = -70.9870237 ; }
        else if (acct.equals("0027")) { lat = 41.663811 ; lon = -70.9870237 ; }
        else if (acct.equals("0028")) { lat = 41.6637224 ; lon = -70.98709699999999 ; }
        else if (acct.equals("0029")) { lat = 41.6629344 ; lon = -70.9881152 ; }
        else if (acct.equals("0030")) { lat = 41.6637224 ; lon = -70.98709699999999 ; }
        else if (acct.equals("0031")) { lat = 41.6637224 ; lon = -70.98709699999999 ; }
        else if (acct.equals("0032")) { lat = 41.663811 ; lon = -70.9870237 ; }
        else if (acct.equals("0034")) { lat = 41.6372717 ; lon = -70.9701207 ; }
        else if (acct.equals("0036")) { lat = 41.661548 ; lon = -71.04019699999999 ; }
        else if (acct.equals("0037")) { lat = 41.66416359999999 ; lon = -70.9917191 ; }
        else if (acct.equals("0039")) { lat = 41.6727975 ; lon = -70.9894229 ; }
        else if (acct.equals("0040")) { lat = 41.6725929 ; lon = -70.9872446 ; }
        else if (acct.equals("0041")) { lat = 41.6527293 ; lon = -70.9869634 ; }
        else if (acct.equals("0042")) { lat = 41.6522752 ; lon = -70.9882542 ; }
        else if (acct.equals("0043")) { lat = 41.6535028 ; lon = -70.98781079999999 ; }
        else if (acct.equals("0044")) { lat = 41.6535912 ; lon = -70.9858395 ; }
        else if (acct.equals("0045")) { lat = 41.65163769999999 ; lon = -70.9862217 ; }
        else if (acct.equals("0046")) { lat = 41.6395996 ; lon = -70.9866026 ; }
        else if (acct.equals("0047")) { lat = 41.6395884 ; lon = -70.9865968 ; }
        else if (acct.equals("0048")) { lat = 41.6395683 ; lon = -70.9865864 ; }
        else if (acct.equals("0049")) { lat = 41.6499918 ; lon = -70.9872785 ; }
        else if (acct.equals("0050")) { lat = 41.6409049 ; lon = -70.998052 ; }
        else if (acct.equals("0051")) { lat = 41.6436883 ; lon = -70.9998165 ; }
        else if (acct.equals("0053")) { lat = 41.641451 ; lon = -71.0002159 ; }
        else if (acct.equals("0054")) { lat = 41.6431899 ; lon = -71.001351 ; }
        else if (acct.equals("0055")) { lat = 41.63920299999999 ; lon = -70.9878827 ; }
        else if (acct.equals("0056")) { lat = 41.6411593 ; lon = -70.9863503 ; }
        else if (acct.equals("0057")) { lat = 41.6409471 ; lon = -70.99939859999999 ; }
        else if (acct.equals("0058")) { lat = 41.64190749999999 ; lon = -71.00791079999999 ; }
        else if (acct.equals("0059")) { lat = 41.6416155 ; lon = -71.00351359999999 ; }
        else if (acct.equals("0060")) { lat = 41.6395899 ; lon = -70.9856592 ; }
        else if (acct.equals("0061")) { lat = 41.6396075 ; lon = -71.0185839 ; }
        else if (acct.equals("0062")) { lat = 41.6369335 ; lon = -71.0024703 ; }
        else if (acct.equals("0063")) { lat = 41.6404998 ; lon = -71.0069015 ; }
        else if (acct.equals("0064")) { lat = 41.6383385 ; lon = -71.0338084 ; }
        else if (acct.equals("0065")) { lat = 41.637087 ; lon = -71.03963999999999 ; }
        else if (acct.equals("0066")) { lat = 41.639121 ; lon = -70.9820299 ; }
        else if (acct.equals("0067")) { lat = 41.6385792 ; lon = -70.9794502 ; }
        else if (acct.equals("0068")) { lat = 41.63948269999999 ; lon = -70.9921548 ; }
        else if (acct.equals("0069")) { lat = 41.6394784 ; lon = -70.9916904 ; }
        else if (acct.equals("0071")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0072")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0073")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0075")) { lat = 41.6201 ; lon = -71.031707 ; }
        else if (acct.equals("0077")) { lat = 41.638478 ; lon = -70.970119 ; }
        else if (acct.equals("0080")) { lat = 41.639594 ; lon = -70.9865997 ; }
        else if (acct.equals("0081")) { lat = 41.6403594 ; lon = -70.9827748 ; }
        else if (acct.equals("0082")) { lat = 41.6390043 ; lon = -70.9804176 ; }
        else if (acct.equals("0083")) { lat = 41.638019 ; lon = -71.0141079 ; }
        else if (acct.equals("0085")) { lat = 41.6200068 ; lon = -70.9746019 ; }
        else if (acct.equals("0086")) { lat = 41.6396901 ; lon = -70.98358739999999 ; }
        else if (acct.equals("0091")) { lat = 41.673502 ; lon = -70.998228 ; }
        else if (acct.equals("0092")) { lat = 41.6468527 ; lon = -71.00789929999999 ; }
        else if (acct.equals("0094")) { lat = 41.6680511 ; lon = -70.9870933 ; }
        else if (acct.equals("0095")) { lat = 41.64166669999999 ; lon = -70.9913889 ; }
        else if (acct.equals("0096")) { lat = 41.66112870000001 ; lon = -70.9912234 ; }
        else if (acct.equals("0113")) { lat = 41.664234 ; lon = -70.984398 ; }
        else if (acct.equals("0116")) { lat = 41.6623484 ; lon = -70.98419179999999 ; }
        else if (acct.equals("0117")) { lat = 41.6358134 ; lon = -71.04173449999999 ; }
        else if (acct.equals("0118")) { lat = 41.6368304 ; lon = -71.0091807 ; }
        else if (acct.equals("0120")) { lat = 41.6401523 ; lon = -70.98591499999999 ; }
        else if (acct.equals("0121")) { lat = 41.6425573 ; lon = -70.9854565 ; }
        else if (acct.equals("0123")) { lat = 41.6465104 ; lon = -71.0042277 ; }
        else if (acct.equals("0124")) { lat = 41.6370015 ; lon = -70.9662838 ; }
        else if (acct.equals("0125")) { lat = 41.6533261 ; lon = -71.0044741 ; }
        else if (acct.equals("0126")) { lat = 41.639667 ; lon = -70.9822621 ; }
        else if (acct.equals("0128")) { lat = 41.6378229 ; lon = -70.96553159999999 ; }
        else if (acct.equals("0130")) { lat = 41.6716814 ; lon = -70.9902551 ; }
        else if (acct.equals("0131")) { lat = 41.6381387 ; lon = -70.96793819999999 ; }
        else if (acct.equals("0132")) { lat = 41.651088 ; lon = -70.98512699999999 ; }
        else if (acct.equals("0133")) { lat = 41.6358677 ; lon = -70.9641398 ; }
        else if (acct.equals("0134")) { lat = 41.6383105 ; lon = -70.9811624 ; }
        else if (acct.equals("0135")) { lat = 41.6358677 ; lon = -70.9641398 ; }
        else if (acct.equals("0136")) { lat = 41.6358677 ; lon = -70.9641398 ; }
        else if (acct.equals("0137")) { lat = 41.6358677 ; lon = -70.9641398 ; }
        else if (acct.equals("0138")) { lat = 41.639147 ; lon = -70.986881 ; }
        else if (acct.equals("0139")) { lat = 41.6480288 ; lon = -70.9876364 ; }
        else if (acct.equals("0142")) { lat = 41.65998099999999 ; lon = -70.990876 ; }
        else if (acct.equals("0143")) { lat = 41.6670198 ; lon = -70.98611819999999 ; }
        else if (acct.equals("0144")) { lat = 41.668916 ; lon = -70.99228269999999 ; }
        else if (acct.equals("0145")) { lat = 41.674167 ; lon = -70.98532 ; }
        else if (acct.equals("0146")) { lat = 41.6419666 ; lon = -70.9926996 ; }
        else if (acct.equals("0149")) { lat = 41.6424452 ; lon = -70.9921083 ; }
        else if (acct.equals("0151")) { lat = 41.6642194 ; lon = -71.0406182 ; }
        else if (acct.equals("0152")) { lat = 41.6636213 ; lon = -70.989769 ; }
        else if (acct.equals("0153")) { lat = 41.6630702 ; lon = -71.0365923 ; }
        else if (acct.equals("0212")) { lat = 41.6600315 ; lon = -70.98964730000002 ; }
        else if (acct.equals("0214")) { lat = 41.6634219 ; lon = -70.9915408 ; }
        else if (acct.equals("0215")) { lat = 41.6626393 ; lon = -70.9915619 ; }
        else if (acct.equals("0217")) { lat = 41.6606227 ; lon = -71.0375153 ; }
        else if (acct.equals("0221")) { lat = 41.636356 ; lon = -70.967793 ; }
        else if (acct.equals("0222")) { lat = 41.6260502 ; lon = -71.0351094 ; }
        else if (acct.equals("0223")) { lat = 41.66256 ; lon = -71.031959 ; }
        else if (acct.equals("0228")) { lat = 41.6369292 ; lon = -71.01833669999999 ; }
        else if (acct.equals("0235")) { lat = 41.6617939 ; lon = -70.9900567 ; }
        else if (acct.equals("0237")) { lat = 41.66102799999999 ; lon = -70.99173689999999 ; }
        else if (acct.equals("0238")) { lat = 41.665106 ; lon = -71.0347835 ; }
        else if (acct.equals("0241")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0278")) { lat = 41.6374302 ; lon = -70.9621566 ; }
        else if (acct.equals("0292")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("0310")) { lat = 41.62790409999999 ; lon = -70.9551023 ; }
        else if (acct.equals("0312")) { lat = 41.6283702 ; lon = -70.96485609999999 ; }
        else if (acct.equals("0313")) { lat = 41.6265267 ; lon = -70.9594315 ; }
        else if (acct.equals("0314")) { lat = 41.6345839 ; lon = -70.967427 ; }
        else if (acct.equals("0315")) { lat = 41.6287235 ; lon = -70.9654746 ; }
        else if (acct.equals("0316")) { lat = 41.6262482 ; lon = -70.960887 ; }
        else if (acct.equals("0317")) { lat = 41.6244884 ; lon = -70.9561456 ; }
        else if (acct.equals("0321")) { lat = 41.645281 ; lon = -71.00881799999999 ; }
        else if (acct.equals("0322")) { lat = 41.64562859999999 ; lon = -71.00610859999999 ; }
        else if (acct.equals("0324")) { lat = 41.6258797 ; lon = -70.9635112 ; }
        else if (acct.equals("0326")) { lat = 41.6485236 ; lon = -71.0100914 ; }
        else if (acct.equals("0327")) { lat = 41.6241127 ; lon = -70.98222 ; }
        else if (acct.equals("0328")) { lat = 41.637957 ; lon = -70.984573 ; }
        else if (acct.equals("0329")) { lat = 41.6606227 ; lon = -71.0375153 ; }
        else if (acct.equals("0331")) { lat = 41.6412801 ; lon = -70.9929906 ; }
        else if (acct.equals("0332")) { lat = 41.6648772 ; lon = -70.99228990000002 ; }
        else if (acct.equals("0333")) { lat = 41.6680313 ; lon = -70.98983299999999 ; }
        else if (acct.equals("0334")) { lat = 41.6665365 ; lon = -70.9907617 ; }
        else if (acct.equals("0335")) { lat = 41.6657124 ; lon = -70.9949484 ; }
        else if (acct.equals("0336")) { lat = 41.6404532 ; lon = -71.0179989 ; }
        else if (acct.equals("0401")) { lat = 41.6394706 ; lon = -70.99911999999999 ; }
        else if (acct.equals("0402")) { lat = 41.6399743 ; lon = -70.9990968 ; }
        else if (acct.equals("0409")) { lat = 41.6339205 ; lon = -70.9886231 ; }
        else if (acct.equals("0411")) { lat = 41.6395898 ; lon = -70.98659760000001 ; }
        else if (acct.equals("0412")) { lat = 41.6358134 ; lon = -71.04173449999999 ; }
        else if (acct.equals("0413")) { lat = 41.6378135 ; lon = -71.0412521 ; }
        else if (acct.equals("0414")) { lat = 41.6398733 ; lon = -70.9970769 ; }
        else if (acct.equals("0975")) { lat = 41.5930322 ; lon = -70.98042079999999 ; }
        else if (acct.equals("1111")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("1234")) { lat = 42.1584324 ; lon = -71.1447732 ; }
        else if (acct.equals("2222")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("3333")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("4567")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("4748")) { lat = 41.64254722222222 ; lon = -71.00627222222222 ; }
        else if (acct.equals("5135")) { lat = 41.6398558 ; lon = -70.982798 ; }
        else if (acct.equals("5331")) { lat = 41.6796747 ; lon = -71.03069409999999 ; }
        else if (acct.equals("9999")) { lat = 41.6130323 ; lon = -70.9704787 ; }
        else if (acct.equals("syst")) { lat = 41.6130323 ; lon = -70.9704787 ; }

	if (lat != 0.0 && lon != 0.0) {
	    log.info("lat=" + lat);
	    log.info("lon=" + lon);

	    var pos = new Position(lon, lat);
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

    private int get_address(String msg, ParsedMessage pm) {
	var p = Pattern.compile(".* Name:(.*)");
	var m = p.matcher(msg);

	if (m.matches()) {
	    log.info("address=" + m.group(1));
	    pm.Address = m.group(1);
	}

	return 0;
    }
}
