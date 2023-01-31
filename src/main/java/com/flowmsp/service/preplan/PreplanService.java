package com.flowmsp.service.preplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.controller.location.LocationUploadResponse;
import com.flowmsp.db.HydrantDao;
import com.flowmsp.db.LocationDao;
import com.flowmsp.domain.Address;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.hydrant.HydrantRef;
import com.flowmsp.domain.location.Building;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.PolygonUtil;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreplanService {
    private final HydrantDao  hydrantDao;
    private final LocationDao locationDao;
    private final MessageService messageService;
    private static final Logger log = LoggerFactory.getLogger(PreplanService.class);

    public PreplanService(HydrantDao hydrantDao, LocationDao locationDao, MessageService messageService) {
        this.hydrantDao  = hydrantDao;
        this.locationDao = locationDao;
        this.messageService = messageService;
    }

    public Preplan preplan(Customer customer, Location location, boolean autoAccept) {
        Preplan p;
	var rnd = customer.settings.preplanningAreaRounding;

	log.info("preplan: autoAccept=" + autoAccept + " location.requiredFlow=" + location.requiredFlow);
	log.info("preplan: preplanningAreaRounding=" + customer.settings.preplanningAreaRounding);

        if(location.requiredFlow == null) {
            p = preplan(customer, location.geoOutline, location.storey, location.storeyBelow);
        }
        else {
            p = new Preplan();
            p.geoOutline = location.geoOutline;
            p.storey = location.storey;
            p.storeyBelow = location.storeyBelow;
            p.roofArea = Math.round(PolygonUtil.areaLatLonPolygon(location.geoOutline)/rnd) * rnd;
            p.totalArea = p.roofArea;

            if ((p.storey + p.storeyBelow) > 1) {
            	p.totalArea = p.totalArea * (p.storey + p.storeyBelow);
            }

            p.requiredFlow = location.requiredFlow;
            p.planningCenter = PolygonUtil.getCenter(location.geoOutline);
            p.hydrants = determineHydrants(p.planningCenter, p.requiredFlow, customer);
        }

        if(autoAccept) {
            location.roofArea     = p.roofArea;
            location.requiredFlow = p.requiredFlow;
            location.hydrants = p.hydrants.stream().map(h -> h.id).collect(Collectors.toList());
            locationDao.replaceById(location.id, location);
        }

        return p;
    }

    public Preplan preplan(Customer customer, Polygon polygon, int storey, int storeyBelow) {
        Preplan p = new Preplan();
	var rnd = customer.settings.preplanningAreaRounding;

	log.info("preplan: " + customer + " " + polygon + " storey=" + storey + " storeyBelow=" + storeyBelow);

        p.geoOutline = polygon;
        p.storey = storey;
        p.storeyBelow = storeyBelow;
        p.roofArea = Math.round(PolygonUtil.areaLatLonPolygon(polygon)/rnd) * rnd;
        p.totalArea = p.roofArea;
        p.requiredFlow = Math.min(p.totalArea, customer.settings.preplanningMaxAreaForFlowComputation) / 3;

        if (p.storey + p.storeyBelow > 1) {
	    p.totalArea = p.totalArea * (p.storey + p.storeyBelow);
	    p.requiredFlow   = Math.min(p.totalArea, (customer.settings.preplanningMaxAreaForFlowComputation * (p.storey + p.storeyBelow))) / 3;

	    if (p.requiredFlow > customer.settings.preplanningMaxAreaForFlowComputation) {
		p.requiredFlow = (long) customer.settings.preplanningMaxAreaForFlowComputation;
	    }
        }

        p.planningCenter = PolygonUtil.getCenter(polygon);
        p.hydrants = determineHydrants(p.planningCenter, p.requiredFlow, customer);

        return p;
    }

    private List<HydrantRef> determineHydrants(Point planningCenter, Long requiredFlow, Customer customer) {
        List<HydrantRef> results = new ArrayList<>();
        int totalFlow = 0;
	double max_distance = customer.settings.preplanningMaxDistanceForHydrantSearch;
        List<Hydrant> hydrants = hydrantDao.getNear(planningCenter, feetToMeters((double)customer.settings.preplanningMaxDistanceForHydrantSearch));

	log.info("determineHydrants: planningCenter=" + planningCenter);
	log.info("determineHydrants: requiredFlow=" + requiredFlow);
	log.info("determineHydrants: customer=" + customer);
	log.info("determineHydrants: max_distance=" + max_distance);
	log.info("determineHydrants: hydrants.size()=" + hydrants.size());

        for (int x = 0; x < customer.settings.preplanningMaxHydrants && x < hydrants.size() && totalFlow < requiredFlow; ++x) {
            Hydrant h = hydrants.get(x);
            HydrantRef hr = new HydrantRef();
            hr.id = h.id;
            results.add(hr);

	    log.info("determineHydrants: h=" + h.id + " " + "flow=" + h.flow);

            if(h.flow == null) {
                totalFlow += 500;
            }
            else {
                totalFlow += h.flow;
            }
        }

        return results;
    }

    private Double feetToMeters(Double feet) {
        return feet * 0.3048;
    }
    
    private String[] splitByNumber(String str, int size) {
        return (size<1 || str==null) ? null : str.split("(?<=\\G.{"+size+"})");
    }
    
    private void AppendInfo(StringBuilder sb, String msg) {
    	//Break the message into 60 character per line
	sb.append("NOTE: " + msg + System.lineSeparator());

    	//msg = "[Info]:@" + DateTime.now().toString("dd-MMM-yyyy HH:mm:ss") + "@:" + msg;
    	//String[] msgArr = splitByNumber(msg, 120);
    	//for(int ii = 0; ii < msgArr.length; ii++) {
    	//	sb.append(msgArr[ii] + System.lineSeparator());	
    	//}
    }
    private void AppendError(StringBuilder sb, String msg) {
	sb.append("WARN: " + msg + System.lineSeparator());

    	//msg = "[Err]:@" + DateTime.now().toString("dd-MMM-yyyy HH:mm:ss") + "@:" + msg;
    	//String[] msgArr = splitByNumber(msg, 120);
    	//for(int ii = 0; ii < msgArr.length; ii++) {
    	//	sb.append(msgArr[ii] + System.lineSeparator());	
    	//}
    }
    
    private void AppendMessage(StringBuilder sb, String msg) {
	sb.append("INFO: " + msg + System.lineSeparator());
    	//msg = "[Msg]:@" + DateTime.now().toString("dd-MMM-yyyy HH:mm:ss") + "@:" + msg;
    	//String[] msgArr = splitByNumber(msg, 120);
    	//for(int ii = 0; ii < msgArr.length; ii++) {
    	//	sb.append(msgArr[ii] + System.lineSeparator());	
    	//}
    }
    
    private int FillLocationFields(Location loc, String[] data) {
    	int pos = 0;
    	//Name Street Address	Street Address2	City	State	Zip code
    	loc.name = data[++pos];
    	loc.address.address1 = data[++pos];
    	loc.address.address2 = data[++pos];
    	loc.address.city = data[++pos];
    	loc.address.state = data[++pos];
    	loc.address.zip = data[++pos];
    	
    	//Roof Area (Sq. Ft)
    	loc.roofArea = parseLong(data[++pos]);
    	//Occupancy Type	Construction Type	Roof Type	Roof Construction	Roof Material	
    	loc.building.occupancyType = data[++pos];
    	loc.building.constructionType = data[++pos];
    	loc.building.roofType = data[++pos];
    	loc.building.roofConstruction = data[++pos];
    	loc.building.roofMaterial = data[++pos];
    	
    	//Normal Population	Sprinklered	Stand Pipe	Fire Alarm	Hours of Operation	
    	loc.building.normalPopulation = data[++pos];
    	loc.building.sprinklered = data[++pos];
    	loc.building.standPipe = data[++pos];
    	loc.building.fireAlarm = data[++pos];
    	loc.building.hoursOfOperation = data[++pos];
    	
    	//Owner Contact	Owner Phone	Notes
    	loc.building.ownerContact = data[++pos];
    	loc.building.ownerPhone = data[++pos];
    	loc.building.notes = data[++pos];
    	loc.storey = parseStorey(data[++pos]);
    	loc.storeyBelow = parseStorey(data[++pos]);
    	return pos;
    }
    
    private boolean validateLatitude(StringBuilder sb, int LineNumber, double lat) {
    	//Latitude from 19.50139 to 64.85694 
    	if (lat >= 19.50139 && lat <= 64.85694) {
    		return true;
    	}
    	AppendError(sb, "Error in record line:" + LineNumber + " -Coordinate are not in USA: Latitude:" + lat);
    	return false;
    }

    private boolean validateLongitude(StringBuilder sb, int LineNumber, double lon) {
    	//longitude from -161.75583 to -68.01197.
    	if (lon >= -161.75583 && lon <= -68.01197) {
    		return true;
    	}
    	AppendError(sb, "Error in record line:" + LineNumber + " - Coordinate are not in USA: Longitude:" + lon);
    	return false;
    }

    private boolean validateLocationOption(StringBuilder sb, int LineNumber, String option, String value) {
    	String validateOption  = getLocationOption(option, value);
    	if (validateOption.equalsIgnoreCase("-1")) {
    		AppendError(sb, "Error in record line:" + LineNumber + " - Invalid Option:" + option + " with value:" + value);
    		return false;
    	} else if (validateOption.equalsIgnoreCase("-2")) {
    		AppendError(sb, "Error in record line:" + LineNumber + " - Invalid Option Validation:" + option);
    		return false;
    	}
    	return true;
    }
    
    private String getLocationOption(String option, String value) {
    	if (value == null) {
    		return "";
    	}
    	if (value.isEmpty()) {
    		return "";
    	}
    	
    	String[] availableOptions = new String [] {};
    	if (option.equalsIgnoreCase("Occupancy Type")) {    		
    		/*availableOptions = new String [] {"Assembly", "Board & Care", "Business",
    				"Day-Care", "Detention & Correctional", "Educational",
    				"High Hazard", "Industrial", "Medical Care / Institutional",
    				"Mercantile", "Multi-Family", "Residential",
    				"Special Structures", "Storage"
    				};*/
    		/*availableOptions = new String [] {"Assembly", "Mercantile, Business", "Detention & Correctional / Medical Care, Institutional",
    				"Residential, Board and Care, Daycare", "Educational", "Industrial", 
    				"Manufacturing and Processing", "Multi-Family",
    				"Special Structures, High Hazard", "Storage"
    				};*/
    		availableOptions = new String [] {"Assembly", "Board & Care", "Business / Mercantile",
			"Day-Care", "Detention & Correctional", "Educational", "High Hazard",
			"Industrial", "Medical Care / Institutional",
			"Mercantile", "Multi-Family", "Residential",
			"Special Structures", "Storage"
			};
    	} else if (option.equalsIgnoreCase("Construction Type")) {
    		availableOptions = new String[] {"Not Classified",
    				"Type IA - Fire Resistive, Non-combustible",
    				"Type IB - Fire Resistive, Non-combustible",
    				"Type IIA - Protective, Non-combustible",
    				"Type IIB - Unprotective, Non-combustible",
    				"Type IIIA - Protected Ordinary",
    				"Type IIIB - Unprotected Ordinary",
    				"Type IV - Heavy Timber",
    				"Type VA - Protected Combustible",
    				"Type VB - Unprotected Combustible"
    				};
    	} else if (option.equalsIgnoreCase("Roof Type")) {
    		availableOptions = new String[] {"Bonnet", "Bowstring Truss", "Butterfly",
    				"Combination", "Curved", "Dome",
    				"Flat", "Gable", "Gambrel",
    				"Hip", "Jerkinhead", "Mansard",
    				"Pyramid", "Saltbox", "Sawtooth", "Skillion"
    				};
    	} else if (option.equalsIgnoreCase("Roof Construction")) {
    		availableOptions = new String[] {"Beam - Concrete",
    				"Beam - Steel", "Beam - Wood",
    				"Steel Truss - Open Web",
    				"Wood / Steel - Closed Web", "Wood / Steel - Open Web",
    				"Wood Truss - Closed Web", "Wood Truss - Open Web"
    				};
    	} else if (option.equalsIgnoreCase("Roof Material")) {
    		availableOptions = new String[] {"Built-Up",
    				"Composition Shingles",
    				"Membrane - Plastic elastomer",
    				"Membrane - Synthetic elastomer",
    				"Metal",
    				"Metal - Corrugated",
    				"Metal - Shake",
    				"Metal - Standing Seam",
    				"Roof Covering Not Class",
    				"Roof Covering Undetermined/Not Reported",
    				"Shingle - Asphalt / Composition",
    				"Slate - Composition",
    				"Slate - Natural",
    				"Structure Without Roof",
    				"Tile - Clay",
    				"Tile - Concrete",
    				"Tile (clay, cement, slate, etc.)",
    				"Wood - Shingle/Shake",
    				"Wood Shakes/Shingles (Treated)",
    				"Wood Shakes/Shingles (Untreated)"
    				};
    	} else if (option.equalsIgnoreCase("Sprinklered")) {
    		availableOptions = new String[] {"Dry System", "Wet System", "Both", "None"};
    	} else if (option.equalsIgnoreCase("Stand Pipe")) {
    		availableOptions = new String[] {"Yes", "No"};
    	} else if (option.equalsIgnoreCase("Fire Alarm")) {
    		availableOptions = new String[] {"Yes", "No"};
    	} else if (option.equalsIgnoreCase("Normal Population")) {
    		availableOptions = new String[] {"Vacant",
    				"1 - 10",
    				"11 - 50",
    				"51 - 100",
    				"101 - 500",
    				"501 - 1000"
    				};
    	} else {
    		return "-2";
    	}
    	
    	String ValueWithoutSpace = value.replaceAll(" ", "");
    	
    	for (int ii = 0; ii < availableOptions.length; ii ++) {
    		
    		if (ValueWithoutSpace.equalsIgnoreCase(availableOptions[ii].replaceAll(" ", ""))) {
    			return availableOptions[ii];
    		}
    	}
    	return "-1"; //Not Found
    }
    
    public void uploadPrePlan(InputStream inputStream, Customer customer, String userID, ObjectMapper objectmapper, LocationUploadResponse returnResponse) {
    	StringBuilder sb = new StringBuilder();
	Date now = new Date();
        CsvParserSettings settings = new CsvParserSettings();
        //the file used in the example uses '\n' as the line separator sequence.
        //the line separator sequence is defined here to ensure systems such as MacOS and Windows
        //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
        settings.getFormat().setLineSeparator("\n");
        // creates a CSV parser
        CsvParser parser = new CsvParser(settings);
        parser.beginParsing(inputStream);

    	long batchNo = System.currentTimeMillis();
    	int LineNumber = 1;
    	try {
	    AppendInfo(sb, "Process started. Batch number:" + batchNo);

	    String[] headerArr;
            headerArr = parser.parseNext();
            if (headerArr == null) {
            	AppendError(sb, "File header is null");
            	return;
            }
            String header = "";
            for (int ii = 0; ii < headerArr.length; ii ++) {
            	header = header + headerArr[ii].replace("﻿", "") + ",";
            }
            header = header.substring(0, header.length() - 1);
            
            char ch1 = header.charAt(0);
            if (!((ch1 >= 'a' && ch1 <= 'z') || (ch1 >= 'A' && ch1 <= 'Z'))) {
            	header = header.substring(1);
            }

            ch1 = header.charAt(0);
            if (!((ch1 >= 'a' && ch1 <= 'z') || (ch1 >= 'A' && ch1 <= 'Z'))) {
            	header = header.substring(1);
            }

            String headerConstant = "Polygon,Name,Street Address,Street Address2,City,State,Zip code,Roof Area (Sq. Ft),Occupancy Type,Construction Type,Roof Type,Roof Construction,Roof Material,Normal Population,Sprinklered,Stand Pipe,Fire Alarm,Hours of Operation,Owner Contact,Owner Phone,Notes,Storey Above,Storey Below";
            if (!header.equalsIgnoreCase(headerConstant)) {
            	AppendError(sb, "Header Incorrect, Found:" + header + ": Expecting:" + headerConstant);
            	return;
            }

	    String[] data;
            while ((data = parser.parseNext()) != null) {
            	LineNumber++;
            	//AppendInfo(sb,"Processing record line:" + LineNumber);
            	try {
		    if (data.length <= 0) {
			AppendError(sb, "Skipping blank record line:" + LineNumber);
			returnResponse.recordKountFail++;
			continue;
		    }
		    Boolean allNull = true;
		    //If All NULL then also continue
		    for (int ii = 0; ii < data.length; ii++) {
			if (data[ii] != null) {
			    allNull = false;
			    break;
			}
		    }
		    if (allNull) {
			AppendError(sb, "Skipping null record line:" + LineNumber);
			returnResponse.recordKountFail++;
			continue;
		    }

		    Location loc = new Location();
                    loc.id = UUID.randomUUID().toString();
                    loc.customerId   = customer.id;
                    loc.customerSlug = customer.slug;
		    loc.building = new Building();
		    loc.address = new Address();
                	
		    FillLocationFields(loc, data);
		    int InsertOrUpdate = 0;
		    //Check Address first
		    //let us check if this polygon already exist
		    Optional<Location> locFind = locationDao.getByFieldValue("address", loc.address);
		    if (locFind.isPresent()) {
			loc = locFind.get();

			if (loc.batchNo == batchNo) {
			    AppendError(sb, "Duplicate location (same address) Error in record line:" + LineNumber);
			    returnResponse.recordKountFail++;
			    continue;
			}
			AppendInfo(sb,"Location already existed (address found) record line:" + LineNumber);
                    	loc.building = new Building();
                    	FillLocationFields(loc, data);
                    	InsertOrUpdate = 1;
		    } else {
                    	//First 1 is Polygon
                    	int numofPoints = 0;
                    	int polygonClosed = -1;
                    	
                    	String polygonStr = NVLString(data[0]);
                    	
                    	String[] couplePoint = polygonStr.split("\\|");
                    	int polygonKount = couplePoint.length;
                    	if (polygonKount <= 1) {
			    long AreaSqFt = 400; //100ft X 100ft
			    if (polygonStr.equalsIgnoreCase("A")) {
				//We don't have coordinates for this. We need to create a standard size square here.
				AreaSqFt = loc.roofArea;
				if (AreaSqFt <= 0) {
				    AreaSqFt = 400; //100ft X 100ft
				}
			    }
			    //First I need Address and then Its Lat & Lon
			    String addressToSearch = NVLAddress(loc.address);
			    Point midPt = messageService.getPointByGoogleMaps(customer, addressToSearch);
			    if (midPt == null) {
				//Can't do anything with this
				AppendError(sb, "Error in record line:" + LineNumber + " - can't extract latlon against address:" + addressToSearch);
				returnResponse.recordKountFail++;
				continue;
			    }
			    long length = (long) Math.sqrt(AreaSqFt);
			    double distanceinFeet = (1.0 / 1.41421356237) * length;
			    distanceinFeet = (int)distanceinFeet;
			    if (distanceinFeet <= 0) {
				distanceinFeet = 50;
			    }
			    Position midPos = midPt.getPosition();
			    double earthRadiusinFeet = 20902263.78;
			    double midLat = midPos.getValues().get(1);
			    double midLon = midPos.getValues().get(0);
			    GeoLocation midLoc = GeoLocation.fromDegrees(midLat, midLon);
			    GeoLocation[] boundedBox = midLoc.boundingCoordinates(distanceinFeet, earthRadiusinFeet);
			    //Lets get 4 coordinates and then we are done with polygon
			    double x1 = boundedBox[0].getLatitudeInDegrees();
			    double y1 = boundedBox[0].getLongitudeInDegrees();

			    double x3 = boundedBox[1].getLatitudeInDegrees();
			    double y3 = boundedBox[1].getLongitudeInDegrees();

			    double x2 = x1;
			    double y2 = y3;

			    double x4 = x3;
			    double y4 = y1;

			    polygonStr = x1 + ":" + y1 + "|" + x2 + ":" + y2 + "|" + x3 + ":" + y3 + "|" + x4 + ":" + y4;
			    couplePoint = polygonStr.split("\\|");
			    polygonKount = couplePoint.length;
			    //AppendInfo(sb, "AutoPolygon created against record line:" + LineNumber + " @Polygon-[" + polygonStr + "] @latLon-" + midLat + ":" + midLon + " @Area-" + AreaSqFt + " @Address-" + addressToSearch);
                    	}
                    	double[] lat = new double[polygonKount];
                    	double[] lon = new double[polygonKount];
                    	String positionError = "";
                    	for (int ii = 0; ii < couplePoint.length; ii ++) {
			    if (couplePoint[ii].isEmpty()) {
				continue;
			    }
			    String[] latLon = couplePoint[ii].split("\\:");
			    if (latLon.length < 2) {
				//Not a couplet
				positionError = "Error in record line:" + LineNumber + " - invalid geolocation(lat & lon)";
				break;
			    }
			    lat[ii] = parseLatLon(latLon[0]);
			    lon[ii] = parseLatLon(latLon[1]);
			    for (int jj = 0; jj < numofPoints; jj ++) {
				//Check for duplicacy
				if (ii == jj) {
				    continue;
				}
				if (lat[jj] == lat[ii] && lon[jj] == lon[ii]) {
				    if (jj == 0) {
					polygonClosed = 0;
				    } else {
					polygonClosed = 1;
				    }
				    break;
				}
			    }
			    if (polygonClosed >= 0) {
				break;
			    }
			    numofPoints++;
                    	}
                    	if (!positionError.isEmpty() ) {
			    AppendError(sb, positionError);
			    returnResponse.recordKountFail++;
			    continue;
                    	}
                    	if (polygonClosed > 0) {
			    //Polygon Closed but not with first lat lon
			    AppendError(sb, "Error in record line:" + LineNumber + " - closed polygon but not with first lat lon");
			    returnResponse.recordKountFail++;
			    continue;
                    	}
                    	if (numofPoints < 3) {
			    //Polygon points are too less
			    AppendError(sb, "Error in record line:" + LineNumber + " - too few points in polygon");
			    returnResponse.recordKountFail++;
			    continue;
                    	}

                    	Boolean invalidCoordinates = false;
                    	String geoOutLine = "[";
                    	for (int ii = 0; ii < numofPoints; ii ++) {
			    if (!validateLatitude(sb, LineNumber, lat[ii])) {
				invalidCoordinates = true;
				break;
			    }
			    if (!validateLongitude(sb, LineNumber, lon[ii])) {
				invalidCoordinates = true;
				break;
			    }
			    geoOutLine += "{\"latitude\":" + lat[ii] + ",\"longitude\":" + lon[ii] + "},";
                    	}
                    	geoOutLine += "{\"latitude\":" + lat[0] + ",\"longitude\":" + lon[0] + "}";
                    	geoOutLine += "]";
                    	
                    	if (invalidCoordinates) {
			    AppendError(sb, "Error in record line:" + LineNumber + " - Invalid Coordinates");
			    returnResponse.recordKountFail++;
			    continue;
                    	}
                    	
                    	try {
			    loc.geoOutline = objectmapper.readValue(geoOutLine, com.mongodb.client.model.geojson.Polygon.class);
			} catch (Exception e) {
			    AppendError(sb, "Error in record line:" + LineNumber + " - invalid polygon:" + e);
			    returnResponse.recordKountFail++;
			    continue;
			}

                    	//let us check if this polygon already exist
                    	Optional<Location> locFindGeo = locationDao.getByFieldValue("geoOutline", loc.geoOutline);
                    	if (locFindGeo.isPresent()) {                    		
			    loc = locFindGeo.get();
			    if (loc.batchNo == batchNo) {
				AppendError(sb, "Duplicate location (same polygon) Error in record line:" + LineNumber);
				returnResponse.recordKountFail++;
				continue;
			    }

			    loc.building = new Building();
			    loc.address = new Address();
			    FillLocationFields(loc, data);
			    InsertOrUpdate = 1;
			    AppendInfo(sb,"Location already existed record line:" + LineNumber);
                    	}               		
		    }
		    if (NVLString(loc.address.address1).isEmpty()) {
			AppendError(sb, "Error in record line:" + LineNumber + " - Address1 can't be NULL");
                    	returnResponse.recordKountFail++;
                    	continue;
		    }
		    if (NVLString(loc.address.city).isEmpty()) {
			AppendError(sb, "Error in record line:" + LineNumber + " - City can't be NULL");
                    	returnResponse.recordKountFail++;
                    	continue;
		    }
		    if (NVLString(loc.address.state).isEmpty()) {
			AppendError(sb, "Error in record line:" + LineNumber + " - State can't be NULL");
                    	returnResponse.recordKountFail++;
                    	continue;
		    }
                    if (!validateLocationOption(sb, LineNumber, "Occupancy Type", loc.building.occupancyType)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Construction Type", loc.building.constructionType)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Roof Type", loc.building.roofType)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Roof Construction", loc.building.roofConstruction)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Roof Material", loc.building.roofMaterial)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Sprinklered", loc.building.sprinklered)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Stand Pipe", loc.building.standPipe)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Fire Alarm", loc.building.fireAlarm)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (!validateLocationOption(sb, LineNumber, "Normal Population", loc.building.normalPopulation)) {
                    	returnResponse.recordKountFail++;
                    	continue;
                    }
                    if (loc.storey > 30) {
                    	AppendError(sb, "Error in record line:" + LineNumber + " - Invalid Option:Storey Above with value:" + loc.storey);
                    	returnResponse.recordKountFail++;
                    	continue;                    	
                    }
                    if (loc.storeyBelow > 30) {
                    	AppendError(sb, "Error in record line:" + LineNumber + " - Invalid Option:Storey Below with value:" + loc.storeyBelow);
                    	returnResponse.recordKountFail++;
                    	continue;                    	
                    }
                    Preplan pp = preplan(customer, loc.geoOutline, loc.storey, loc.storeyBelow);

                    if(loc.roofArea == null || loc.roofArea == 0) {
                        loc.roofArea = pp.roofArea;
                    }
                    if(loc.requiredFlow == null || loc.requiredFlow == 0) {
                        loc.requiredFlow = pp.requiredFlow;
                    }
                    if (loc.hydrants == null || loc.hydrants.size() == 0) {
                        loc.hydrants = new ArrayList<>();
                        for(HydrantRef hr : pp.hydrants) {
                            loc.hydrants.add(hr.id);
                        }
                    }                    
                    
		    if (InsertOrUpdate <= 0) {
			loc.createdOn = now;
                        loc.modifiedOn = now;
                        loc.createdBy = userID;
                        loc.modifiedBy = userID;
                        loc.batchNo = batchNo;
			locationDao.save(loc);
			//AppendMessage(sb, "Location created against record line:" + LineNumber);
			returnResponse.recordKountInsert++;
		    } else {
                        loc.modifiedOn = now;
                        loc.modifiedBy = userID;
                        loc.batchNo = batchNo;
			locationDao.replaceById(loc.id, loc);
			AppendMessage(sb, "Location updated against record line:" + LineNumber);
			returnResponse.recordKountUpdate++;
		    }
            	} catch (Exception loopError) {
		    AppendError(sb, "Error in record line:" + LineNumber + " - Exception:" + loopError);
		    returnResponse.recordKountFail++;
		    continue;
            	}
            }

            returnResponse.successFlag = 0;
            returnResponse.msg = "Upload process completed";

    	} catch (Exception ex) {
	    returnResponse.successFlag = 99;
	    AppendError(sb, "Unhandled Exception causes error at record line:" + LineNumber + " Internal error:" + ex);
	    returnResponse.msg = "Exception occured while processing:system message:" + ex;
    	} finally {
	    AppendInfo(sb, "Process completed.");
	    returnResponse.recordKount = returnResponse.recordKountInsert + returnResponse.recordKountUpdate + returnResponse.recordKountFail;
	    AppendInfo(sb, "Record(s) processed:" + returnResponse.recordKount);
	    AppendInfo(sb, "Record(s) inserted:" + returnResponse.recordKountInsert);
	    AppendInfo(sb, "Record(s) updated:" + returnResponse.recordKountUpdate);
	    AppendInfo(sb, "Record(s) failed:" + returnResponse.recordKountFail);
	    returnResponse.log = sb.toString();
	    try {
		parser.stopParsing();
	    } catch(Exception i) {
	    }
    	}
    }
    
    private Double parseLatLon(String data) {
        try {
            return Double.parseDouble(data);
        }
        catch(Exception e) {
            return null;
        }
    }
    
    private int parseStorey(String data) {
        try {
            return Integer.parseInt(data);
        }
        catch(Exception e) {
            return 0;
        }
    }

    private Double parseDouble(String data) {
        try {
            return Double.parseDouble(data);
        }
        catch(Exception e) {
            return 0.0;
        }
    }
    
    private long parseLong(String data) {
        try {
            return Long.parseLong(data);
        }
        catch(Exception e) {
            return 0L;
        }
    }
    
    private String NVLString(String data) {
    	if (data == null) {
    		return "";
    	}
    	return data;
    }
    
    private String NVLAddress(Address add) {
    	if (add == null) {
    		return "";
    	}
    	String address = NVLString(add.address1);
    	if (!NVLString(add.address2).isEmpty()) {
    		address += " " + NVLString(add.address2);	
    	}
    	if (!NVLString(add.city).isEmpty()) {
    		address += " " + NVLString(add.city);	
    	}
    	if (!NVLString(add.state).isEmpty()) {
    		address += " " + NVLString(add.state);	
    	}
    	if (!NVLString(add.zip).isEmpty()) {
    		address += " " + NVLString(add.zip);	
    	}
    	return address;
    }
}
