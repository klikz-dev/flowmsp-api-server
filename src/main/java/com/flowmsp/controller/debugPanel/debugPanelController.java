package com.flowmsp.controller.debugPanel;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.jooq.lambda.function.Function0;
import org.jooq.lambda.function.Function2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.domain.debugpanel.debugInfo;
import com.flowmsp.domain.debugpanel.debugUser;
import com.flowmsp.service.debugpanel.GeneralService;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import spark.Request;
import spark.Response;

public class debugPanelController {
	private final Function2<Request, debugUser, debugPanelUserResponse> responseBuilder = debugPanelUserResponse::build;
    private static final Logger log = LoggerFactory.getLogger(debugPanelController.class);

    private final DebugInfoDao debugInfoDao;
    private final GeneralService generalService;
    private final ObjectMapper objectMapper;
    
    public debugPanelController(GeneralService generalService, DebugInfoDao debugInfoDao, ObjectMapper objectMapper) {
    	this.generalService = generalService;
        this.debugInfoDao = debugInfoDao;
        this.objectMapper = objectMapper;
    }
   
    public debugPanelUserListResponse getAllUsersList(Request req, Response res) {
    	debugPanelUserListResponse ret = new debugPanelUserListResponse();
    	try {
    		List<debugUser> usrList = generalService.getAllUsers();
    		return usrList.stream()
    				.map(responseBuilder.applyPartially(req))
                    .collect(debugPanelUserListResponse::new,
                    		debugPanelUserListResponse::accept,
                    		debugPanelUserListResponse::combine);
    	}
    	catch (Exception ex) {
    		
    	}
    	return ret;
    }
    
    /*
     * Get a debugPanelInfo List. If
     * there is any exception throw during processing a 500 error is returned.
     */
    public debugPanelListResponse get(Request req, Response res) {
        try {
        	debugPanelListResponse ret = new debugPanelListResponse();        	
        	Function0<List<debugInfo>> query = null;
            String source = req.queryParams("source");
            String customer = req.queryParams("customer");
            String timeFrom = req.queryParams("timeFrom");
            String timeTill = req.queryParams("timeTill");
            
            long timeFromL = Long.parseLong(timeFrom);
            long timeTillL = Long.parseLong(timeTill);
            
            if (timeFromL > timeTillL) {
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in debgPanelListResponse.get Query time from is greater than time till");
                ret.setServerMessage("Invalid duration. Date from can't be greater than date till");
                return ret;
            }
            
            Date timestampFrom1 = new Date(timeFromL);            
            Date timestampTill1 = new Date(timeTillL);            
            
            Calendar cal = Calendar.getInstance();            
            cal.setTime(timestampFrom1);
            
            // Set time fields to zero
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date timestampFrom2 = cal.getTime();
            
            cal.setTime(timestampTill1);
            // Set time fields to zero
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            Date timestampTill2 = cal.getTime();
            
            DateTime dt1 = new DateTime(timestampFrom2);
    		DateTime dt2 = new DateTime(timestampTill2);
    		
            int daysDiff = Days.daysBetween(dt1, dt2).getDays();
            if (daysDiff > 3) {
            	//Can't Query data more than 3 days
            	//Source not set. This is an error
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in debgPanelListResponse.get Query More than 3 Days");
                ret.setServerMessage("Duration more than 3 days is not supported.");
                return ret;
            }
            
            Date timestampFrom = dt1.minusDays(1).toDate();
            Date timestampTill = dt2.plusHours(24).toDate();
            
            if (source.equalsIgnoreCase("DISPATCH")) {
            	if (customer.equalsIgnoreCase("ALL")) {
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "DISPATCH"), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	} else {
            		//A Specific Customer
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "DISPATCH"), Filters.eq("Details.Customer", customer), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	}
            } else if (source.equalsIgnoreCase("ACTIVITY")) {
            	if (customer.equalsIgnoreCase("ALL")) {
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.ne("Source", "DISPATCH"), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	} else {
            		//A Specific Customer
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.ne("Source", "DISPATCH"), Filters.eq("Details.Customer", customer) , Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	}            	
            } else if (source.equalsIgnoreCase("WEB")) {
            	if (customer.equalsIgnoreCase("ALL")) {
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "Web"), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	} else {
            		//A Specific Customer
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "Web"), Filters.eq("Details.Customer", customer), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	}  
            } else if (source.equalsIgnoreCase("MOBILE")) {
            	if (customer.equalsIgnoreCase("ALL")) {
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.regex("Source", "^Mobile"), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	} else {
            		//A Specific Customer
            		query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.regex("Source", "^Mobile"), Filters.eq("Details.Customer", customer), Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            	}
            } else {
            	//Source not set. This is an error
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            	ret.setServerMessage("Source is not set in query.");
                return ret;
            }            
            
            return query.apply()
                    .stream()
                    .map(debugPanelResponse.builder().applyPartially(req))
                    .collect(debugPanelListResponse::new,
                    		debugPanelListResponse::accept,
                    		debugPanelListResponse::combine);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in debgPanelListResponse.get", e);
            return new debugPanelListResponse();
        }
    }
    
    public debugPanelListResponse getDashboard(Request req, Response res) {
        try {
        	debugPanelListResponse ret = new debugPanelListResponse();        	
        	Function0<List<debugInfo>> query = null;
            String source = req.queryParams("source");
            String timeFrom = req.queryParams("timeFrom");
            String timeTill = req.queryParams("timeTill");
            
            long timeFromL = Long.parseLong(timeFrom);
            long timeTillL = Long.parseLong(timeTill);
            
            if (timeFromL > timeTillL) {
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in debgPanelListResponse.get Query time from is greater than time till");
                ret.setServerMessage("Invalid duration. Date from can't be greater than date till");
                return ret;
            }
            
            Date timestampFrom1 = new Date(timeFromL);            
            Date timestampTill1 = new Date(timeTillL);            
            
            Calendar cal = Calendar.getInstance();            
            cal.setTime(timestampFrom1);
            
            // Set time fields to zero
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date timestampFrom2 = cal.getTime();
            
            cal.setTime(timestampTill1);
            // Set time fields to zero
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
            Date timestampTill2 = cal.getTime();
            
            DateTime dt1 = new DateTime(timestampFrom2);
    		DateTime dt2 = new DateTime(timestampTill2);
    		
            int daysDiff = Days.daysBetween(dt1, dt2).getDays();
            if (daysDiff > 5) {
            	//Can't Query data more than 5 days
            	//Source not set. This is an error
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in debgPanelListResponse.get Query More than 3 Days");
                ret.setServerMessage("Duration more than 3 days is not supported.");
                return ret;
            }
            
            Date timestampFrom = dt1.minusDays(1).toDate();
            Date timestampTill = dt2.plusHours(24).toDate();
            
            if (source.equalsIgnoreCase("DISPATCH")) {
            	
            	query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "DISPATCH"),  Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            } else if (source.equalsIgnoreCase("ACTIVITY")) {            	
            	query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.ne("Source", "DISPATCH"),  Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            } else if (source.equalsIgnoreCase("WEB")) {
            	query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.eq("Source", "Web"),  Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            } else if (source.equalsIgnoreCase("MOBILE")) {
            	query = () -> debugInfoDao.getAllByFilter(Filters.and(Filters.regex("Source", "^Mobile"),  Filters.gte("TimeStamp", timestampFrom), Filters.lte("TimeStamp", timestampTill)));
            } else {
            	//Source not set. This is an error
            	res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            	ret.setServerMessage("Source is not set in query.");
                return ret;
            }            
            
            
            return query.apply()
                    .stream()
                    .map(debugPanelResponse.builder().applyPartially(req))
                    .collect(debugPanelListResponse::new,
                    		debugPanelListResponse::accept,
                    		debugPanelListResponse::combine);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in debgPanelListResponse.get", e);
            return new debugPanelListResponse();
        }
    }
}