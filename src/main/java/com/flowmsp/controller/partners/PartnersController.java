package com.flowmsp.controller.partners;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.jooq.lambda.function.Function2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.db.DispatchRegisterDao;
import com.flowmsp.db.PartnersDao;
import com.flowmsp.domain.DispatchRegister;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.partners.Partners;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.partners.partnersService;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;

import spark.Request;
import spark.Response;

public class PartnersController {
		private final Function2<Request, Partners, PartnersResponse> responseBuilder = PartnersResponse::build;
		private static final Logger log = LoggerFactory.getLogger(PartnersController.class);
		
		private final PartnersDao   partnersDao;
		private final CustomerDao   customerDao;
		private final DispatchRegisterDao dispatchRegisterDao;
		private final ObjectMapper  objectMapper;
		private final partnersService partnersService;
		private final DebugInfoDao debugInfoDao;
		
	    public PartnersController(PartnersDao   partnersDao, DispatchRegisterDao dispatchRegisterDao, partnersService partnersService, CustomerDao customerDao, DebugInfoDao debugInfoDao, ObjectMapper mapper) {
	    	this.partnersDao = partnersDao;
	    	this.dispatchRegisterDao = dispatchRegisterDao; 
	    	this.partnersService = partnersService;
	        this.customerDao   = customerDao;
	        this.objectMapper = mapper;
	        this.debugInfoDao = debugInfoDao;
	    }
	    
	    public PartnersListResponse getAll(Request req, Response res) {
	    	
            ValidatedProfile profile;
			try {
				profile = ProfileUtil.getValidatedProfile(req, res);
				String customerId = profile.getCustomerId();
				Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
				boolean iAmAdmin = false;
				if (customer.license != null) {
					if (customer.license.licenseType != null) {
						if (customer.license.licenseType == LicenseType.Master) {
							iAmAdmin = true;
						}
					}
				}
				List<Partners> partnersAll = partnersDao.getAllByFieldValue("customerId", customerId);
				List<Partners> partnersConsent = new ArrayList<Partners>();
				for (int ii = 0; ii < partnersAll.size(); ii ++) {
					Partners p = partnersAll.get(ii);
            		Optional<Customer> partnerCustomer = customerDao.getById(p.partnerId);
            		if (!partnerCustomer.isPresent()) {
            			continue;
            		}
            		Customer partnerCust = partnerCustomer.get();
            		if (partnerCust.dataSharingConsent || iAmAdmin) {
            			partnersConsent.add(p);
            		}            		
				}
				PartnersListResponse response = partnersConsent
		                                                   .stream()
		                                                   .map(responseBuilder.applyPartially(req))
		                                                   .collect(PartnersListResponse::new,
		                                                		   PartnersListResponse::accept,
		                                                		   PartnersListResponse::combine);
		        return response;
			} catch (Exception e) {
				res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	            log.error("Error in PartnersController.getAll", e);
	            return new PartnersListResponse();
			}
	    }
	    
	    private boolean IsPartnerThere(List<Partners> myPartners, String partnerID) {
	    	for (int ii = 0; ii < myPartners.size(); ii++) {
	    		if (myPartners.get(ii).partnerId.equalsIgnoreCase(partnerID)) {
	    			return true;
	    		}
	    	}
	    	return false;
	    }
	    
	    public PartnersListResponse addPartners(Request req, Response res) {
	    	debugPanel xray = new debugPanel(debugInfoDao);
	        try {
	        	xray.SetCustomerInfo(req, res, "SAVE_PARTNERS");
	        	String jwt = req.headers("Authorization");
	        	jwt = jwt.substring("Bearer".length() + 1).trim();
	        	
	            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
	            String customerId   = profile.getCustomerId();
	            Customer customer   = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            	JSONParser jp = new JSONParser();
            	JSONArray jArr =  (JSONArray) jp.parse(req.body());
            	Set<String> partnerArr = new HashSet<>();
            	
            	for (int ii = 0; ii < jArr.size(); ii ++) {
            		partnerArr.add(jArr.get(ii).toString());
            	}
            	
            	List<Partners> myPartners = partnersDao.getAllByFieldValue("customerId", customerId);
            	//Check if any previous friend is not now in list, delete it
            	for (int ii = 0; ii < myPartners.size(); ii ++) {
            		Partners row = myPartners.get(ii);
            		if (!partnerArr.contains(row.partnerId)) {
            			partnersDao.deleteById(row.id);
            		}
            	}
            	
            	for (String partnerId : partnerArr) {
            		if (!IsPartnerThere(myPartners, partnerId)) {
                		Partners partner = new Partners();
                		partner.id = UUID.randomUUID().toString();
                		partner.customerId = customerId;
                		partner.partnerId = partnerId;                		
                		partnersDao.save(partner);
            		}            	    
            	}
            	
            	if (customer.dispatchSharingConsent) {
            		//We need to register for dispatch as well
            		dispatchRegisterDao.deleteAllByFieldValue("customerId", customer.id);
            		Date now = new Date();
                    List<Partners> partners = partnersDao.getAllByFieldValue("customerId", customer.id);
                    
                    for (int ii = 0; ii < partners.size(); ii ++) {
                        DispatchRegister dr = new DispatchRegister();
                        dr.id = UUID.randomUUID().toString();
                        dr.customerId = customer.id;
                        dr.timeStamp = now;
                        dr.jwt = jwt;
                        dr.partnerId = partners.get(ii).partnerId;
                        dispatchRegisterDao.save(dr);
                    }
            	}
            	
	            res.status(HttpStatus.SC_CREATED);

	            PartnersListResponse response = partnersDao.getAllByFieldValue("customerId", customerId)
                        .stream()
                        .map(responseBuilder.applyPartially(req))
                        .collect(PartnersListResponse::new,
                        		PartnersListResponse::accept,
                        		PartnersListResponse::combine);
	            return response;
	        }
	        catch(Exception e) {
	            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	            log.error("Error creating location", e);
	        } finally {
	        	xray.commitLog();
	        }
	        return new PartnersListResponse();
	    }
}
