package com.flowmsp.controller.customer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.LinkRelation;
import com.flowmsp.controller.LinkRelationUtil;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.db.DispatchRegisterDao;
import com.flowmsp.db.PartnersDao;
import com.flowmsp.domain.Address;
import com.flowmsp.domain.DispatchRegister;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.partners.Partners;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.service.BsonUtil;
import com.flowmsp.service.ServerSendEventHandler;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.partners.partnersService;
import com.flowmsp.service.patch.PatchNotAllowedException;
import com.flowmsp.service.patch.PatchUtil;
import com.flowmsp.service.signup.SignupRequest;
import com.flowmsp.service.signup.SignupResult;
import com.flowmsp.service.signup.SignupService;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.mongodb.client.model.geojson.Point;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jetty.http.HttpStatus;
import org.jooq.lambda.function.Function3;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;

public class CustomerController {
    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final Function3<Request, UserRole, Customer, CustomerResponse> responseBuilder = CustomerResponse::build;

    private final CustomerDao   customerDao;
    private final DispatchRegisterDao dispatchRegisterDao;
    private final SignupService signupService;
    private final ObjectMapper  objectMapper;
    private final partnersService partnersService;
    private final PartnersDao partnersDao;
    private final DebugInfoDao debugInfoDao;
    
    public CustomerController(CustomerDao customerDao, DispatchRegisterDao dispatchRegisterDao, DebugInfoDao debugInfoDao, partnersService partnersService, PartnersDao partnersDao, SignupService signupService, ObjectMapper mapper) {
        this.customerDao   = customerDao;
        this.dispatchRegisterDao = dispatchRegisterDao;
        this.partnersService = partnersService;
        this.partnersDao = partnersDao;
        this.signupService = signupService;
        this.objectMapper = mapper;
        this.debugInfoDao = debugInfoDao;
    }

    private UserRole getUserRole(Request req, Response res) {
        final SparkWebContext context = new SparkWebContext(req, res);        
        final ProfileManager manager  = new ProfileManager(context);
        final Optional<CommonProfile> profile = manager.get(false);
        Set<String> roles = profile.get().getRoles();
        if(roles.contains(UserRole.ADMIN.toString())) {
            return UserRole.ADMIN;
        }
        else if (roles.contains(UserRole.PLANNER.toString())) {
            return UserRole.PLANNER;
        }
        else {
            return UserRole.USER;
        }
    }

    /*
     * Get a single customer
     */
    public CustomerResponse get(Request req, Response res) {
        String id = req.params("id");
        Optional<Customer> c = customerDao.getById(id);

        return c.map(responseBuilder.applyPartially(req, getUserRole(req, res)))
                .orElseGet(() -> {
                    res.status(404);
                    return responseBuilder.apply(req, null, null);
                });
    }

    /*
     * Gets all the entities
     */
    public CustomerListResponse getAll(Request req, Response res) {
        CustomerListResponse response = customerDao.getAll()
                                                   .stream()
                                                   .map(responseBuilder.applyPartially(req, getUserRole(req, res)))
                                                   .collect(CustomerListResponse::new,
                                                            CustomerListResponse::accept,
                                                            CustomerListResponse::combine);
        return response;
    }

    /*
     * Gets all the entities which are in radius
     */
    public CustomerListResponse getAllRadius(Request req, Response res) {
        String id = req.params("id");
        String radius = req.params("radius");
        Optional<Customer> c = customerDao.getById(id);
        if (!c.isPresent() ) {
			res.status(HttpStatus.NOT_IMPLEMENTED_501);
            log.error("Customer Not Found in getAllRadius = " + id);
            return new CustomerListResponse();
        }
        Customer myCustomer = c.get();
        if (myCustomer.address == null) {
			res.status(HttpStatus.NOT_IMPLEMENTED_501);
            log.error("Customer Address Not Found in getAllRadius = " + id);
            return new CustomerListResponse();
        }
        if (myCustomer.address.latLon == null) {
			res.status(HttpStatus.NOT_IMPLEMENTED_501);
            log.error("Customer LatLon Not Found in getAllRadius = " + id);
            return new CustomerListResponse();
        }
        double maxLength = Double.parseDouble(radius); //in miles
       
        List<Customer> customers = customerDao.getAll();
        List<Partners> partners = partnersDao.getAllByFieldValue("customerId", myCustomer.id);
        List<Customer> potentialPartners = new ArrayList<Customer>();
        for (int ii = 0; ii < customers.size(); ii ++) {
        	Customer row = customers.get(ii);
        	if (row.id.equalsIgnoreCase(myCustomer.id)) {
        		continue;
        	}
        	//Check if it is already a partner, then no need to check radius
        	boolean isPartner = false;
        	for (int jj = 0; jj < partners.size(); jj ++) {
        		Partners partnerRow = partners.get(jj);
        		if (partnerRow.partnerId.equalsIgnoreCase(row.id)) {
        			isPartner = true;
        			break;
        		}
        	}
        	if (isPartner) {
        		potentialPartners.add(row);
        	} else {
            	if (row.address.latLon == null) {
            		continue;
            	}
            	double distanceInMeter = Distance(myCustomer.address.latLon, row.address.latLon);
            	if (distanceInMeter > maxLength || distanceInMeter < 0) {
            		continue;
            	}
            	potentialPartners.add(row);        		
        	}
        }
        CustomerListResponse response = potentialPartners.stream()
                                                   .map(responseBuilder.applyPartially(req, getUserRole(req, res)))
                                                   .collect(CustomerListResponse::new,
                                                            CustomerListResponse::accept,
                                                            CustomerListResponse::combine);
        return response;
    }
    
    private double Distance(Point p1, Point p2) {    	
    	double dist = -1.0;
    	try
    	{
        	double startLat, startLong, endLat, endLong;
        	
        	List<Double> p1Dbl = p1.getCoordinates().getValues();
        	startLong = p1Dbl.get(0);
        	startLat = p1Dbl.get(1);
        	
        	List<Double> p2Dbl = p2.getCoordinates().getValues();
        	endLong = p2Dbl.get(0);
        	endLat = p2Dbl.get(1);
        	
    		
    		double dLat  = Math.toRadians((endLat - startLat));
    		double dLong = Math.toRadians((endLong - startLong));

    		startLat = Math.toRadians(startLat);
    		endLat   = Math.toRadians(endLat);

    		double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
    		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    		dist = 6371 * c * 0.621371; // In Miles		
    		
    	} catch(Exception ex) {
    		dist = -1.0;
    	}
		return dist;
    }
	
    private double haversin(double val) {
		return Math.pow(Math.sin(val / 2), 2);
	}
     
    public Map<String, Object> setUIConfig(Request req, Response res)
    {
        String slug = req.params("slug");
        String id   = req.params("id");
        Optional<Customer> c = customerDao.getById(id);

        if(c.isPresent()) {
            Customer customer = c.get();
            try
            {
                Map<String, Object>    d  = objectMapper.readValue(req.body(), new TypeReference<HashMap<String, Object>>() {});
                Map<String, BsonValue> d2 = BsonUtil.convertToBsonValueMap(d);
                Optional<Customer> updatedCustomer = customerDao.addUIConfig(customer.id, d2);
                if(updatedCustomer.isPresent())
                {
                    Map<String, BsonValue> config  = updatedCustomer.get().uiConfig;
                    Map<String, Object>    config2 = BsonUtil.convertToObjectMap(config);
                    config2.put("id", updatedCustomer.get().id);
                    config2.put("links", Lists.newArrayList(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, CustomerResponse.selfLink(req, slug, id))));
                    return config2;
                }
                else
                {
                    res.status(500);
                    return new HashMap<>(0);
                }
            }
            catch(IOException e)
            {
                res.status(500);
                return new HashMap<>(0);
            }
        }
        else {
            res.status(404);
            return new HashMap<>();
        }
    }

    public Map<String, Object> getUIConfig(Request req, Response res) {
        String slug = req.params("slug");
        String id   = req.params("id");
        Optional<Customer> c = customerDao.getById(id);
        if(c.isPresent()) {
            Map<String, Object> config = BsonUtil.convertToObjectMap(c.get().uiConfig);
            config.put("id", c.get().id);
            config.put("links", Lists.newArrayList(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, CustomerResponse.selfLink(req, slug, id))));
            return config;
        }
        else {
            res.status(404);
            return new HashMap<>();
        }
    }

    /**
     * Create a new customer from a SignupRequest provided in the request body.
     */
    public SignupResponse addCustomer(Request req, Response res) {
        // Get the SignupRequest object from the body of the request
        SignupRequest s = null;
        try
        {
            s = objectMapper.readValue(req.body(), new TypeReference<SignupRequest>() {});
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }

        // If there was no body, or the SignupRequest was not able to be created, return an empty response
        // and the BAD_REQUEST error code
        if(s == null) {
            res.status(HttpStatus.BAD_REQUEST_400);
            return SignupResponse.build("Unable to read signup request");
        }
        // If the SignupRequest does not contain the required elements, this request cannot be processed.
        // Return an empty response and the appropriate error code.
        else if(s.customerName == null || s.customerName.isEmpty() ||
                s.email        == null || s.email.isEmpty()    ||
                s.password     == null || s.password.isEmpty()) {
            res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
            return SignupResponse.build("Required Fields Missing");
        }
        // The SignupRequest has been validated, create the new customer by calling the SignupService. Return
        // the CREATED status code. The LOCATION header will contain the URL of the new customer and the
        // response body will contain additional data in the form of a SignupResponse.
        else {
            SignupResult signupResult = signupService.createCustomer(s);

            if(signupResult.customer != null) {
                // There was a customer in the result, so the request was successfully processed.
                SignupResponse sr = SignupResponse.build(req, signupResult.customer, signupResult.user);
                Optional<LinkRelation> self = LinkRelationUtil.getByRelation("customer", sr.links);
                if (self.isPresent()) {
                    res.header(HttpHeaders.LOCATION, self.get().href);
                }
                res.status(HttpStatus.CREATED_201);
                return sr;
            }
            else {
                // There was no customer so the result failed for some reason.
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return SignupResponse.build(signupResult.errorMessage);
            }
        }
    }

    public CustomerResponse updateCustomer(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	try {
    		xray.SetCustomerInfo(req, res, "UPDATE_CUSTOMER_INFO");
	        String id = req.params("id");
	        Optional<Customer> c = customerDao.getById(id);
	
	        if(c.isPresent()) {
	            Customer customer = c.get();
	            try {
	                Map<String, BsonValue> store = customer.uiConfig;
	                customer.uiConfig = null;
	                if (customer.address == null) {
	                	customer.address = new Address();
	                }

	                log.info("body");
	                log.info(req.body());

	                log.info(customer.toString());
	                Customer patchedCustomer = PatchUtil.patch(req.body(), customer, Customer.class);
	                patchedCustomer.uiConfig = store;
	                if (signupService.UpdateCustomer(customer, patchedCustomer, false)) {
	                    res.header("Location", CustomerResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
	                    return responseBuilder.applyPartially(req, getUserRole(req, res), patchedCustomer).apply();                	
	                } else {
	                    res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
	                    return responseBuilder.apply(req, null, null);
	                }
	            }
	            catch(PatchNotAllowedException e)
	            {
	                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
	                log.error("Error updating customer", e);
	                return responseBuilder.apply(req, null, null);
	            }
	            catch(Exception e) {
	                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
	                log.error("Error updating customer", e);
	                return responseBuilder.apply(req, null, null);
	            }
	        }
	        else {
	            res.status(HttpStatus.NOT_FOUND_404);
	            return responseBuilder.apply(req, null, null);
	        }
    	} finally {
    		xray.commitLog();
    	}    	
    }
    
    public CustomerResponse updateCustomerLatLon(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	try {
    		xray.SetCustomerInfo(req, res, "UPDATE_CUSTOMER_GEOLOCATION");
            String id = req.params("id");
            Optional<Customer> c = customerDao.getById(id);

            if(c.isPresent()) {
                Customer customer = c.get();
                try {
                	if (signupService.UpdateCustomer(customer, customer, true)) {
                        res.header("Location", CustomerResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
                        return responseBuilder.applyPartially(req, getUserRole(req, res), customer).apply();            		
                	} else {
                        res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                        return responseBuilder.apply(req, null, null);
                	}
                }
                catch(Exception e) {
                    res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    log.error("Error updating customer", e);
                    return responseBuilder.apply(req, null, null);
                }
            }
            else {
                res.status(HttpStatus.NOT_FOUND_404);
                return responseBuilder.apply(req, null, null);
            }
    	} finally {
    		xray.commitLog();
    	}
    }
    
    @SuppressWarnings("static-access")
	public CustomerResponse registerMeForDispatch(Request req, Response res) {
    	try {
            String id = req.params("id");
            Optional<Customer> c = customerDao.getById(id);
            if (!c.isPresent() ) {
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                log.error("Error registerMeForDispatch");
                return responseBuilder.apply(req, null, null);        	
            }
            Customer cust = c.get();            
            dispatchRegisterDao.deleteAllByFieldValue("customerId", id);
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(req.body());
			JSONObject jb = (JSONObject) obj;
			String jwt = (String) jb.get("jwt");
            Date now = new Date();
            List<Partners> partners = partnersDao.getAllByFieldValue("customerId", id);
            
            for (int ii = 0; ii < partners.size(); ii ++) {
                DispatchRegister dr = new DispatchRegister();
                dr.id = UUID.randomUUID().toString();
                dr.customerId = id;
                dr.timeStamp = now;
                dr.jwt = jwt;
                dr.partnerId = partners.get(ii).partnerId;
                dispatchRegisterDao.save(dr);
            }
            cust.dispatchSharingConsent = true;
            Bson newValue = new Document("dispatchSharingConsent", true);
            Bson updateOperationDocument = new Document("$set", newValue);
            customerDao.updateById(id, updateOperationDocument);      
            ServerSendEventHandler.GetMyInstance().ReloadRegisteredDispatch();
            res.header("Location", CustomerResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
            return responseBuilder.applyPartially(req, getUserRole(req, res), cust).apply();
    	} catch (Exception ex) {
    		log.error("Exception registerMeForDispatch", ex);
    		res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
    	}
    	return responseBuilder.apply(req, null, null);
    }
    
    @SuppressWarnings("static-access")
	public CustomerResponse deRegisterMeForDispatch(Request req, Response res) {
    	try {
            String id = req.params("id");
            Optional<Customer> c = customerDao.getById(id);
            if (!c.isPresent() ) {
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                log.error("Error registerMeForDispatch");
                return responseBuilder.apply(req, null, null);
            }
            Customer cust = c.get();
            cust.dispatchSharingConsent = false;
            dispatchRegisterDao.deleteAllByFieldValue("customerId", id);
            Bson newValue = new Document("dispatchSharingConsent", false);
            Bson updateOperationDocument = new Document("$set", newValue);
            customerDao.updateById(id, updateOperationDocument); 
            ServerSendEventHandler.GetMyInstance().ReloadRegisteredDispatch();            
            res.header("Location", CustomerResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
            return responseBuilder.applyPartially(req, getUserRole(req, res), cust).apply();    		
    	} catch (Exception ex) {
    		log.error("Exception deregisterMeForDispatch", ex);
    		res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
    	}
    	return responseBuilder.apply(req, null, null);
    }
}
