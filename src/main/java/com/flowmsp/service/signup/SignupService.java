package com.flowmsp.service.signup;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.flowmsp.ApiServer;
import com.flowmsp.SlugContext;
import com.flowmsp.db.*;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.License;
import com.flowmsp.domain.customer.LicenseTerm;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.user.User;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.service.user.UserRequest;
import com.flowmsp.service.user.UserResult;
import com.flowmsp.service.user.UserService;
import com.google.common.base.Strings;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

//import com.flowmsp.service.preplan.
import com.flowmsp.service.preplan.GeoLocation;

import freemarker.template.Template;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of creation of new customers, including all of the administrative notifications.
 */
public class SignupService {
    private final CustomerDao       customerDao;
    private final PasswordDao       passwordDao;
    private final HydrantDao        hydrantDao;
    private final LocationDao       locationDao;
    private final UserDao           userDao;
    private final MessageDao        messageDao;
    private final UserService       userService;
    private final AmazonSNS         sns;
    private final AmazonSimpleEmailService ses;
    
    private final String systemEmail;
    private final String mapAPIKey;
    
    private static final ExecutorService notifyService = Executors.newCachedThreadPool();
    private static final Logger log = LoggerFactory.getLogger(SignupService.class);

    public SignupService(CustomerDao customerDao,
                         PasswordDao passwordDao,
                         HydrantDao  hydrantDao,
                         LocationDao locationDao,
                         UserDao     userDao,
			 MessageDao  messageDao,
                         UserService userService,
                         AmazonSNS sns,
                         AmazonSimpleEmailService ses,
                         String systemEmail,
                         String mapAPIKey) {
        this.customerDao = customerDao;
        this.passwordDao = passwordDao;
        this.hydrantDao  = hydrantDao;
        this.locationDao = locationDao;
        this.userDao     = userDao;
	this.messageDao  = messageDao;
        this.userService = userService;
        this.sns = sns;
        this.ses = ses;
        this.systemEmail = systemEmail;
        this.mapAPIKey = mapAPIKey;
    }

    public SignupResult createCustomer(SignupRequest signupRequest) {
        log.info("Creating new customer with signup request {}", signupRequest);

        // All of the elements of the signup request must be present, or it is an error.
        if(Strings.isNullOrEmpty(signupRequest.customerName) ||
           Strings.isNullOrEmpty(signupRequest.email) ||
           Strings.isNullOrEmpty(signupRequest.firstName) ||
           Strings.isNullOrEmpty(signupRequest.lastName) ||
           Strings.isNullOrEmpty(signupRequest.address.address1) ||
           Strings.isNullOrEmpty(signupRequest.address.city) ||
           Strings.isNullOrEmpty(signupRequest.address.state) ||
           Strings.isNullOrEmpty(signupRequest.address.zip) ||
           Strings.isNullOrEmpty(signupRequest.password)) {
            log.error("Signup failed, missing rquired fields");
            return new SignupResult("Missing required fields");
        }
        // The email address is going to be used as the initial username, therefore it cannot already be used
        // as a username. If it is, we will abort processing and return with an error.
        Optional<Password> existingUser = passwordDao.getByUsername(signupRequest.email.toLowerCase());
        if(existingUser.isPresent()) {
            log.error("Signup failed, duplicate username");
            return new SignupResult("Duplicate username");
        }

        // It looks like we have the right data to create the customer, so use a service to look up the latLon based on
        // the zip code.

        // Create the customer, a customer is always given a one month preview license
        Customer c = new Customer();
        c.id      = UUID.randomUUID().toString();
        c.slug    = customerDao.makeSlug(signupRequest.customerName);
        c.name    = signupRequest.customerName;
        c.address = signupRequest.address;
        c.license = new License(LicenseType.Preview, LicenseTerm.Monthly);
        c.license.creationTimestamp   = Date.from(ZonedDateTime.now().toInstant());
        c.license.expirationTimestamp = Date.from(ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).plusDays(30).toInstant());

        // Create the collections and save the customer
        SlugContext.setSlug(c.slug);
        createCollections();
        customerDao.save(c);

        //Forcefully update geolocations
        UpdateCustomer(c, c, true);

	// distance in miles
	createBoundingBox(c, 100);
        UpdateCustomer(c, c, true);

        // Create the user
        UserRequest userRequest = new UserRequest();
        userRequest.email     = signupRequest.email.toLowerCase();
        userRequest.firstName = signupRequest.firstName;
        userRequest.lastName  = signupRequest.lastName;
        userRequest.password  = signupRequest.password;
        userRequest.role      = UserRole.ADMIN.toString();
        UserResult userResult = userService.addUser(userRequest, c);
        SlugContext.clearSlug();

        // Perform all the necessary notifications
        notifyService.submit(() ->
        {
            try
            {
                // Create the email from the FreeMarker template
                Template template = ApiServer.freeMarkerConfiguration.getTemplate("newCustomer.ftl");
                Writer stringOut = new StringWriter();
                Map<String, Object> model = new HashMap<>();
                model.put("customerName", signupRequest.customerName);
                template.process(model, stringOut);

		var msg = "";
		msg += "Name: " + c.name + "\r\n" ;
		msg += "Slug: " + c.slug + "\r\n" ;
		msg += "Address1: " + c.address.address1 + "\r\n" ;
		msg += "Address2: " + c.address.address2 + "\r\n" ;
		msg += "City: " + c.address.city + "\r\n" ;
		msg += "State: " + c.address.state + "\r\n" ;
		msg += "ZIP Code: " + c.address.zip + "\r\n" ;
		msg += "LAT/LON: " + c.address.latLon + "\r\n" ;
		msg += "Email: " + userRequest.email.toLowerCase() + "\r\n" ;
		msg += "First: " + userRequest.firstName + "\r\n" ;
		msg += "Last: " + userRequest.lastName + "\r\n" ;

		msg += "boundSWLat: " + c.boundSWLat + "\r\n" ;
		msg += "boundSWLon: " + c.boundSWLon + "\r\n" ;
		msg += "boundNELat: " + c.boundNELat + "\r\n" ;
		msg += "boundNELon: " + c.boundNELon + "\r\n" ;

                PublishRequest publishRequest = new PublishRequest();
                //publishRequest.setMessage("New customer signup: " + c.name + " in " + c.address.city + ", " + c.address.state);
                publishRequest.setMessage(msg);
                publishRequest.setTopicArn(System.getenv("SIGNUP_ARN")); // TODO: Get from config
		publishRequest.setSubject("New customer signup");

                SendEmailRequest emailRequest = new SendEmailRequest();
                emailRequest.setDestination(new Destination().withToAddresses(userRequest.email));
                emailRequest.setMessage(new Message().withSubject(new Content().withData("Signup to FlowMSP")).withBody(new Body().withHtml(new Content().withData(stringOut.toString()))));
                emailRequest.setSource(systemEmail);

                if(publishRequest.getTopicArn() == null)
                {
                    //If the environment variable isn't set, warn.
                    log.warn("Signup Topic ARN not set, cannot send SNS notification.");
                }
                else
                {
                    sns.publish(publishRequest);
                }
                ses.sendEmail(emailRequest);
            }
            catch(Throwable t)
            {
                log.error("Error while sending signup notification", t);
            }
        });

        // Return the result
        return new SignupResult(c, userResult.user);
    }

    public void createBoundingBox(Customer c, int miles) {
	log.info("in createBoundingBox");
	long feet_in_mile = 5280 ;
	long length = (long) miles * feet_in_mile;
	double distanceinFeet = (1.0 / 1.41421356237) * length;

	log.info("distanceinFeet=" + distanceinFeet);
	log.info("c.address.latLon=" + c.address.latLon);

	if (c.address.latLon == null) {
	    return;
	}

	log.info("call getPosition()");
	Position midPos = c.address.latLon.getPosition();
	log.info("midPos=" + midPos);
	
	double earthRadiusinFeet = 20902263.78;
	double midLat = midPos.getValues().get(1);
	double midLon = midPos.getValues().get(0);

	log.info("midLat=" + midLat);
	log.info("midLon=" + midLon);
	
	GeoLocation midLoc = GeoLocation.fromDegrees(midLat, midLon);
	log.info("midLoc=" + midLoc);

	GeoLocation[] boundedBox = midLoc.boundingCoordinates(distanceinFeet, earthRadiusinFeet);
	log.info("boundedBox=" + boundedBox);
	log.info("boundedBox[0]=" + boundedBox[0]);
	log.info("boundedBox[1]=" + boundedBox[1]);

	c.boundSWLat = boundedBox[0].getLatitudeInDegrees();
	c.boundSWLon = boundedBox[0].getLongitudeInDegrees();
	c.boundNELat = boundedBox[1].getLatitudeInDegrees();
	c.boundNELon = boundedBox[1].getLongitudeInDegrees();

	log.info("leaving createBoundingBox");

	return;
    }

    public void createCollections() {
        hydrantDao.createCollection();
        locationDao.createCollection();
        userDao.createCollection();
	messageDao.createCollection();
    }

    public void notifyExpiredLicense(User user, Customer customer, License license) {
        log.info("Submitted expired license notify request");
        notifyService.submit(() ->
        {
            try
            {
                log.info("Sending notification of expired license");
                PublishRequest publishRequest = new PublishRequest();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                publishRequest.setMessage("Customer access with expired license. Customer: " + customer.name + ", User: " + user.email + ", Expiration: " + df.format(license.expirationTimestamp));
                publishRequest.setTopicArn(System.getenv("SIGNUP_ARN"));

                if(publishRequest.getTopicArn() == null)
                {
                    //If the environment variable isn't set, warn.
                    log.warn("Signup Topic ARN not set, cannot send SNS notification.");
                }
                else
                {
                    sns.publish(publishRequest);
                }
            }
            catch(Throwable t)
            {
                log.error("Error while sending signup notification", t);
            }
        });
    }
    
    public boolean UpdateCustomer(Customer originalCustomer, Customer patchedCustomer, boolean forceUpdategeoLocation) {
    	try {
    		//Check if geo cordinates are blank then generate geo coordinates
    		//If there is an address change or latLon are null
    		String originalAddress = getAddress(originalCustomer.address);
    		String patchedAddress = getAddress(patchedCustomer.address);
    		if (forceUpdategeoLocation) {
    			patchedCustomer.address.latLon = getPointByGoogleMaps(patchedAddress);
    		} else if (!originalAddress.equalsIgnoreCase(patchedAddress)) {
    			//There is change in address
    			patchedCustomer.address.latLon = getPointByGoogleMaps(patchedAddress);	
    		} else if (IsGeoLocationPresent(originalCustomer.address)) {
    			patchedCustomer.address.latLon = getPointByGoogleMaps(patchedAddress);
    		}        	
    		customerDao.replaceById(patchedCustomer.id, patchedCustomer);    		
    		return true;
    	} catch (Exception ex) {
    		log.error("Error while updating customer", ex);
    	}
    	return false;
    }
    
    public boolean UpdateCustomerGeoLocation(Customer patchedCustomer) {
    	try {
    		//Check if geo cordinates are blank then generate geo coordinates
    		//If there is an address change or latLon are null
    		String patchedAddress = getAddress(patchedCustomer.address);
    		if (!Strings.isNullOrEmpty(patchedAddress)) {
        		if (!IsGeoLocationPresent(patchedCustomer.address)) {
        			patchedCustomer.address.latLon = getPointByGoogleMaps(patchedAddress);
        			customerDao.replaceById(patchedCustomer.id, patchedCustomer);
        		}
    		}
    		return true;
    	} catch (Exception ex) {
    		log.error("Error while UpdateCustomerGeoLocation", ex);
    	}
    	return false;
    }
    
    private boolean IsGeoLocationPresent(com.flowmsp.domain.Address address) {
    	if (address != null) {
    		if (address.latLon != null) {
    			List <Double> coordinates = address.latLon.getCoordinates().getValues();
    			if (coordinates != null) {
    				if (coordinates.size() == 2) {
    					return true;		
    				}
    			}    			
    		}
    		return false;
    	} else {
    		return false;
    	}
    }
    
    private String getAddress(com.flowmsp.domain.Address address) {
    	String retAddress = "";
    	if (address != null) {
        	if (!Strings.isNullOrEmpty(address.address1)) { 
        		retAddress = retAddress + address.address1 + ' ';
        	}
        	if (!Strings.isNullOrEmpty(address.address2)) {
        		retAddress = retAddress + address.address2 + ' ';
        	}
        	if (!Strings.isNullOrEmpty(address.city)) {
        		retAddress = retAddress + address.city + ' ';
        	}
        	if (!Strings.isNullOrEmpty(address.state)) {
        		retAddress = retAddress + address.state + ' ';
        	}
        	retAddress = retAddress.trim();
    	}
    	return retAddress;
    }
    
    @SuppressWarnings("deprecation")
	public Point getPointByGoogleMaps(String addToSearch) {
		String targetURL = "https://maps.googleapis.com/maps/api/geocode/json?address=" + addToSearch.replace(" ", "+")
				+ "&key=" + mapAPIKey;
		log.info("PartnersService HTTP Call:" + targetURL);
		HttpURLConnection connection = null;

		try {
			// Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Content-Type", "application/JSON");
			connection.setRequestProperty("Content-Length", "512");
			connection.setRequestProperty("Content-Language", "en-US");
			connection.setReadTimeout(30000); // set the connection timeout value to 30 seconds (30000 milliseconds)
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				// 200 is for HTTP 200 OK
				log.error("Error timeout by google maps:" + addToSearch);
				return null;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// now parse
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(response.toString());
			JSONObject jb = (JSONObject) obj;

			// now read
			JSONArray jsonObject1 = (JSONArray) jb.get("results");
			if (jsonObject1.size() == 0) {
				// Google Map was unable to produce any result
				return null;
			}
			JSONObject jsonObject2 = (JSONObject) jsonObject1.get(0);
			JSONObject jsonObject3 = (JSONObject) jsonObject2.get("geometry");
			JSONObject location = (JSONObject) jsonObject3.get("location");

			double lat = (double) location.get("lat");
			double lon = (double) location.get("lng");

			return new Point(new Position(lon, lat));
		} catch (Exception e) {
			log.error("Error getting location by google maps", e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
