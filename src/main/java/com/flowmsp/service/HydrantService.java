package com.flowmsp.service;

import com.flowmsp.controller.hydrant.HydrantUploadResponse;
import com.flowmsp.db.HydrantDao;
import com.flowmsp.db.LocationDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.flow.FlowRange;
import com.flowmsp.domain.flow.PinLegend;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.location.Location;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * The relationship between hydrants and locations is a complex one, core to the functionality of the system.
 * The HydrantService implements the methods where this interaction requires more business logic than is avialable
 * in the Dao classes. Methods in this service should be used in preference to methods in the Dao classes if available.
 */
public class HydrantService {
    private static final Logger log = LoggerFactory.getLogger(HydrantService.class);

    private final HydrantDao  hydrantDao;
    private final LocationDao locationDao;

    public HydrantService(HydrantDao hydrantDao, LocationDao locationDao) {
        this.hydrantDao  = hydrantDao;
        this.locationDao = locationDao;
    }

    /**
     * Delete a hydrant given its ID. This process checks to make sure that the hydrant is not associated
     * with any locations before it is deleted. If no hydrant is found with the given id, the optional
     * is returned empty.
     *
     * @param hydrantId
     */
    public Optional<HydrantDeleteResult> deleteByHydrantId(String hydrantId) {
        // Get the hydrant, done to simply validate that the hydrant exists
        Optional<Hydrant> hydrant = hydrantDao.getById(hydrantId);
        if(hydrant.isPresent()) {
            // First check to see if there are any locations associated with this hydrant
            List<Location> locations = locationDao.getAssociatedWithHydrant(hydrantId);
            if (locations.size() == 0) {
                // No locations associated so a deletion can be performed
                hydrantDao.deleteById(hydrantId);
                return Optional.of(new HydrantDeleteResult(true, new ArrayList<>()));
            } else {
                // Since there are locations associated with the hydrant the deletion cannot be performed.
                return Optional.of(new HydrantDeleteResult(false, locations));
            }
        }
        else {
            return Optional.empty();
        }
    }

    /**
     * Delete all hydrants for current user. This process checks to make sure that any hydrant is not associated
     * with a location before it is deleted. If no hydrants are found , the optional is returned empty.
     *
     * @param hydrantId
     */
	public Optional<HydrantDeleteResult> deleteAllHydrants() {
        // Get the hydrant, done to simply validate that the hydrants exists
		List<Hydrant> hydrants = hydrantDao.getAll();
        if(hydrants.size() > 0) {
            // Irrespective of the location the hydrant is attached to, delete them all.
            hydrantDao.deleteAll();
            //Delete hydrant associations from all Locations
            locationDao.updateAll((Bson)new Document("$unset", new Document("hydrants","")));
            
            return Optional.of(new HydrantDeleteResult(true, new ArrayList<>()));
        }
        else {
            return Optional.empty();
        }
    }   
    
    /**
     * Given a hydrant and a customer, set the hydrants flow range based on the customer flow range pin mapping.
     *
     * @param hydrant
     * @param customer
     */
    public void setHydrantFlowRange(Hydrant hydrant, Customer customer) {
        if (hydrant.flow == null) {
            // There is no flow so set it to unknown
            hydrant.flowRange = new FlowRange(customer.pinLegend.unknownPinColor.label, customer.pinLegend.unknownPinColor.pinColor);
        }
        else {
            PinLegend.FlowRange matchedRange = null;
            for (PinLegend.FlowRange flowRange : customer.pinLegend.rangePinColors) {
                if (hydrant.flow >= flowRange.low && hydrant.flow < flowRange.high) {
                    matchedRange = flowRange;
                    break;
                }
            }
            hydrant.flowRange = new FlowRange(matchedRange.label, matchedRange.pinColor);
        }
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

    private void AppendInfo(StringBuilder sb, String msg) {
	sb.append("NOTE: " + msg + System.lineSeparator());
    }
    private void AppendError(StringBuilder sb, String msg) {
	sb.append("WARN: " + msg + System.lineSeparator());
    }
    
    private void AppendMessage(StringBuilder sb, String msg) {
	sb.append("INFO: " + msg + System.lineSeparator());
    }
    
    public void uploadHydrants(InputStream inputStream, Customer customer, String userId, HydrantUploadResponse returnResponse) {
    	StringBuilder sb = new StringBuilder();
    	int LineNumber = 1;
    	long batchNo = System.currentTimeMillis();
    	double minimumNewHydrantDistance = PolygonUtil.feetToMeters(customer.settings.minimumNewHydrantDistance);
	var msgpref = "uploadHydrants: batchNo=" + batchNo + " ";

	log.info(msgpref + "minimumNewHydrantDistance=" + minimumNewHydrantDistance);

    	try {
	    AppendInfo(sb, "Process started. Batch number:" + batchNo + " minimumNewHydrantDistance=" + minimumNewHydrantDistance + "m");
	    CsvParserSettings settings = new CsvParserSettings();
            //the file used in the example uses '\n' as the line separator sequence.
            //the line separator sequence is defined here to ensure systems such as MacOS and Windows
            //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
            settings.getFormat().setLineSeparator("\n");
            Date now = new Date();

            // creates a CSV parser
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(inputStream);

            String[] headerArr;
            headerArr = parser.parseNext();
            if (headerArr == null) {
            	AppendError(sb, "File header is null");
		log.error(msgpref + "File header is null");
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

            String headerConstant = "lat,lon,flow,size,address,inservice,notes,dryhydrant,outservicedate";
            if (!header.equalsIgnoreCase(headerConstant)) {
            	AppendError(sb, "Header Incorrect, Found:" + header + ": Expecting:" + headerConstant);
		log.error(msgpref + "Header Incorrect, Found: " + header);
		log.error(msgpref + "Expecting: " + headerConstant);
            	return;
            }
            
            String[] data;
            		
            while ((data = parser.parseNext()) != null) {
            	LineNumber++;

            	if (data.length <= 0) {
		    AppendError(sb, LineNumber + ": Skipping blank record line");
		    log.warn(msgpref + LineNumber + ": Skipping blank record line");
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
		    AppendError(sb, LineNumber + ": Skipping null record line");
		    log.warn(msgpref + LineNumber + ": Skipping null record line");
		    returnResponse.recordKountFail++;
		    continue;
            	}
            	
            	int pos = 0;
                String latString = data[pos++];
                String lonString = data[pos++];
                if(Strings.isNullOrEmpty(latString) || Strings.isNullOrEmpty(lonString)) {
		    AppendError(sb, LineNumber + ": Skipping blank record line:");
		    log.warn(msgpref + LineNumber + ": Skipping blank record line");
		    returnResponse.recordKountFail++;
		    continue;
                }

                String id = UUID.randomUUID().toString();
                String externalRef = null;
                Double latitude  = parseLatLon(latString);
                Double longitude = parseLatLon(lonString);

		if (!validateLatitude(sb, LineNumber, latitude)) {
		    AppendError(sb, LineNumber + ": Invalid LAT/LON Coordinates");
		    log.error(msgpref + LineNumber + ": Invalid LAT");
		    returnResponse.recordKountFail++;
		    continue;
		}

		if (!validateLongitude(sb, LineNumber, longitude)) {
		    AppendError(sb, LineNumber + ": Invalid LAT/LON Coordinates");
		    log.error(msgpref + LineNumber + ": Invalid LON");
		    returnResponse.recordKountFail++;
		    continue;
		}
        		
            	Integer flow = parseFlow(data[pos++]);
                Integer size = parseInt(data[pos++]);
                String streetAddress = data[pos++];
                Boolean inService = parseBoolean(data[pos++], true);
                String notes = data[pos++];
                Boolean dryHydrant = parseBoolean(data[pos++], false);
                Date outServiceDate = parseDate(data[pos++]);

                //See if it is already there
                Point pt = new Point(new Position(longitude, latitude));
                Optional<Hydrant> hyd = hydrantDao.getByFieldValue("lonLat", pt);
                if (hyd.isPresent()) {
		    //Update hydrant
		    Hydrant hydUpdate = hyd.get();
		    if (hydUpdate.batchNo == batchNo) {
			//This is duplicate
			AppendMessage(sb, LineNumber + ": Duplicate hydrant input record line");
			log.warn(msgpref + LineNumber + ": Duplicate hydrant input record line");
			returnResponse.recordKountFail++;
		    } else {
                        Hydrant hydrant = new Hydrant(hydUpdate.id, customer, externalRef, latitude, longitude, flow, size, streetAddress, inService, outServiceDate, dryHydrant, notes);
                        setHydrantFlowRange(hydrant, customer);
                        hydrant.modifiedBy = userId;
                        hydrant.modifiedOn = now;
                        hydrant.batchNo = batchNo;
                        hydrantDao.replaceById(hydrant.id, hydrant);
			AppendMessage(sb, LineNumber + ": existing hydrant updated");
			log.info(msgpref + LineNumber + ": existing hydrant updated");
			returnResponse.recordKountUpdate++;
		    }
                } else {
                    List<Hydrant> hydrantsExist = hydrantDao.getNear(latitude, longitude, minimumNewHydrantDistance);                
                    if(hydrantsExist == null || hydrantsExist.size() == 0) {
                        Hydrant hydrant = new Hydrant(id, customer, externalRef, latitude, longitude, flow, size, streetAddress, inService, outServiceDate, dryHydrant, notes);
                        setHydrantFlowRange(hydrant, customer);
                        hydrant.createdBy = userId;
                        hydrant.modifiedBy = userId;
                        hydrant.createdOn = now;
                        hydrant.modifiedOn = now;
                        hydrant.batchNo = batchNo;

                        //Last Check If two threads are working
                        if(!hydrantDao.getByFieldValue("lonLat", pt).isPresent()) {
			    hydrantDao.save(hydrant);
			    // log.info(msgpref + LineNumber + " new hydrant created");
			    returnResponse.recordKountInsert++;
                        } else {
			    AppendError(sb, LineNumber + ": Skipping hydrant, it seems same hydrant is created by another thread");
			    log.error(msgpref + LineNumber + ": Skipping hydrant, it seems same hydrant is created by another thread");
			    returnResponse.recordKountFail++;
                        }
                    }
                    else {
                    	Hydrant closeHydrant = hydrantsExist.get(0);
			AppendError(sb, LineNumber + ": Skipping nearby hydrant: " + closeHydrant.streetAddress + " " + closeHydrant.latLon);
			log.error(msgpref + LineNumber + ": Skipping nearby hydrant: " + closeHydrant.streetAddress + " " + closeHydrant.latLon);
			returnResponse.recordKountFail++;
                    }                	
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

	    log.info("uploadHydrants: batchNo=" + batchNo + " " + returnResponse.msg);
	    log.info("uploadHydrants: batchNo=" + batchNo + " processed=" + returnResponse.recordKount);
	    log.info("uploadHydrants: batchNo=" + batchNo + " inserted=" + returnResponse.recordKountInsert);
	    log.info("uploadHydrants: batchNo=" + batchNo + " updated=" + returnResponse.recordKountUpdate);
	    log.info("uploadHydrants: batchNo=" + batchNo + " failed=" + returnResponse.recordKountFail);
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

    private Integer parseInt(String data) {
        try {
            return Integer.parseInt(data);
        }
        catch(Exception e) {
            return null;
        }
    }
    
    private Boolean parseBoolean(String data, Boolean defaultData) {
        try {
        	if (data == null) {
        		return defaultData;
        	}
        	if (data.isEmpty()) {
        		return defaultData;
        	}
        	if (data.equalsIgnoreCase("true")) {
        		return true;
        	} else if (data.equalsIgnoreCase("false")) {
        		return false;
        	}        	
            return Boolean.parseBoolean(data);
        }
        catch(Exception e) {
            return null;
        }
    }

    private Date parseDate(String data) {
        try {
        	if (data == null) {
        		return null;
        	}
        	if (data.isEmpty()) {
        		return null;
        	}
            return new Date(data);
        }
        catch(Exception e) {
            return null;
        }
    }

    private Integer parseFlow(String data) {
    	if (data == null) {
    		return null;
    	}
    	if (data.isEmpty() || data.equalsIgnoreCase("null")) {
    		return null;
    	}
        // Many times the flow strings will have GPM in them, get rid of this
        String s = data.toLowerCase();
        s = s.replaceAll("gpm", "").trim();

        try {
            if (s.contains("-")) {
                // If there is a dash, it is due to a range, in which case the flow becomes the mid point of the range
                String[] parts = s.split("-");
                if (parts.length == 2) {
                    int low = Integer.parseInt(parts[0].trim());
                    int high = Integer.parseInt(parts[1].trim());
                    return (low + high) / 2;
                } else {
                    return null;
                }
            }
            else {
                // Sometimes the data comes in as a float, get rid of anything after a period
                s = s.split("\\.")[0].trim();

                // If there is no dash, remove some other characters that appear and maybe it is a single value
                s = s.replaceAll("\\+", "").trim();
                return Integer.parseInt(s.trim());
            }
        }
        catch(Exception e) {
            return null;
        }
    }
}
