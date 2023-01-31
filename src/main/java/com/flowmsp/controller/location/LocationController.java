package com.flowmsp.controller.location;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.LinkRelation;
import com.flowmsp.controller.LinkRelationUtil;
import com.flowmsp.controller.hydrant.HydrantUploadResponse;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.db.HydrantDao;
import com.flowmsp.db.LocationDao;
import com.flowmsp.db.MessageDao;
import com.flowmsp.db.PartnersDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.hydrant.HydrantRef;
import com.flowmsp.domain.location.Building;
import com.flowmsp.domain.location.Image;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.partners.Partners;
import com.flowmsp.domain.user.User;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.image.ImageService;
import com.flowmsp.service.patch.PatchUtil;
import com.flowmsp.service.preplan.Preplan;
import com.flowmsp.service.preplan.PreplanService;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.google.common.base.Strings;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HeaderElement;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.jooq.lambda.function.Function2;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Updates.unset;

public class LocationController {
    private static final Logger log = LoggerFactory.getLogger(LocationController.class);

    private final Function2<Request, Location, LocationResponse> responseBuilder      = LocationResponse::build;
    private final Function2<Request, Location, LocationResponse> responseBuilderMini      = LocationResponse::buildMini;
    
    private final LocationDao    locationDao;
    private final HydrantDao     hydrantDao;
    private final CustomerDao    customerDao;
    private final UserDao    	 userDao;
    private final MessageDao     messageDao;
    private final PartnersDao	 partnersDao;
    private final ImageService   imageService;
    private final PreplanService preplanService;
    private final ObjectMapper   objectMapper;
    private final MessageService messageService;
    private final DebugInfoDao debugInfoDao;
    
    public LocationController(LocationDao locationDao, HydrantDao hydrantDao, CustomerDao customerDao, UserDao userDao, MessageDao messageDao, PartnersDao partnersDao, ImageService imageService, PreplanService preplanService, MessageService messageService, ObjectMapper objectMapper, DebugInfoDao debugInfoDao) {
        this.locationDao    = locationDao;
        this.hydrantDao		= hydrantDao;
        this.customerDao    = customerDao;
        this.userDao    	= userDao;
        this.messageDao		= messageDao;
        this.partnersDao 	= partnersDao;
        this.imageService   = imageService;
        this.preplanService = preplanService;
        this.messageService = messageService;
        this.objectMapper   = objectMapper;
        this.debugInfoDao = debugInfoDao;
    }

    /*
     * Gets all the locations for a customer.
     *
     * Allows the following query parameters:
     * limit: limits the number of hydrants returned.
     *
     * This will return a list of location responses. If no hydrants are found an empty list
     * is returned. If there is any exc mm, eption thrown during processing a 500 error is returned.
     */
    public LocationListResponse getAll(Request req, Response res) {
        try {
        	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            return 	stream(locationDao.getCollection().find().projection(Projections.fields(Projections.include("id", "customerId",  "customerSlug", "name", "storey", "storeyBelow", "lotNumber", "roofArea", "requiredFlow", "geoOutline", "address", "hydrants", "images", "building"))))
        			.map(responseBuilderMini.applyPartially(req))
        			.collect(LocationListResponse::new,
                    LocationListResponse::accept,
                    LocationListResponse::combine);
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in LocationController.getAll", e);
            return new LocationListResponse();
        }
    }

    //Get one Partner
    public LocationListResponse getPartner(Request req, Response res) {
        try {
        	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
        	String partnerId = req.params("partnerId");
            String customerId = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
    		Optional<Customer> partnerCustomer = customerDao.getById(partnerId);
    		if (!partnerCustomer.isPresent()) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in LocationController.getPartner, Partner Not Found:" + partnerId);
                return new LocationListResponse();    			
    		}
    		Customer partnerCust = partnerCustomer.get();
    		String partnerSlug = partnerCust.slug;
    		if (partnerSlug.equalsIgnoreCase(customerSlug)) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in LocationController.getPartner, Trying to get own data as partner:" + partnerId);
                return new LocationListResponse();
    		}
    		boolean shareData =  customer.dataSharingConsent;
        	boolean iAmAdmin = false;
			if (customer.license != null) {
				if (customer.license.licenseType != null) {
					if (customer.license.licenseType == LicenseType.Master) {
						shareData = true;
						iAmAdmin = true;
					}
				}
			}
			if (!shareData) {
				return new LocationListResponse();
			}
    		SlugContext.setPartnerSlug(customerSlug);
    		SlugContext.setSlug(partnerSlug);
        	//"id", "customerId", "name", "roofArea", "requiredFlow", "geoOutline", "address", "hydrants", "images", "building"            
            return stream(locationDao.getCollection().find().projection(Projections.fields(Projections.include("id", "customerId", "name", "storey", "storeyBelow", "lotNumber", "roofArea", "requiredFlow", "geoOutline", "address", "hydrants", "images", "building"))))
                        .map(responseBuilderMini.applyPartially(req))
                        .collect(LocationListResponse::new,
                                LocationListResponse::accept,
                                LocationListResponse::combine);
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in LocationController.getAll", e);
            return new LocationListResponse();
        }
    }

    private LocationListResponse getPartner(Request req, Response res, String partnerId) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            Optional<Customer> partnerCustomer = customerDao.getById(partnerId);
            if (!partnerCustomer.isPresent()) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in LocationController.getPartner, Partner Not Found:" + partnerId);
                return new LocationListResponse();
            }
            Customer partnerCust = partnerCustomer.get();
            String partnerSlug = partnerCust.slug;
            if (partnerSlug.equalsIgnoreCase(customerSlug)) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in LocationController.getPartner, Trying to get own data as partner:" + partnerId);
                return new LocationListResponse();
            }
            boolean shareData = customer.dataSharingConsent;
            boolean iAmAdmin = false;
            if (customer.license != null) {
                if (customer.license.licenseType != null) {
                    if (customer.license.licenseType == LicenseType.Master) {
                        shareData = true;
                        iAmAdmin = true;
                    }
                }
            }
            if (!shareData) {
                return new LocationListResponse();
            }
            SlugContext.setPartnerSlug(customerSlug);
            SlugContext.setSlug(partnerSlug);
            //"id", "customerId", "name", "roofArea", "requiredFlow", "geoOutline", "address", "hydrants", "images", "building"
            return stream(locationDao.getCollection().find().projection(Projections.fields(Projections.include("id", "customerId", "name", "storey", "storeyBelow", "lotNumber", "roofArea", "requiredFlow", "geoOutline", "address", "hydrants", "images", "building", "customerSlug"))))
                    .map(responseBuilderMini.applyPartially(req))
                    .collect(LocationListResponse::new,
                            LocationListResponse::accept,
                            LocationListResponse::combine);
        } catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in LocationController.getAll", e);
            return new LocationListResponse();
        }
    }

    public MultipleDetailedLocationListResponse getAllPartner(Request req, Response res) {
        List<LocationResponse> locationListResponses = new ArrayList<>();
        MultipleDetailedLocationListResponse multipleDetailedLocationListResponse = new MultipleDetailedLocationListResponse(locationListResponses);

        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Optional<Customer> customer = customerDao.getById(customerId);

            customer.ifPresent(value -> locationListResponses.addAll(getAll(req, res).data));

            List<Partners> partners = partnersDao.getAllByFieldValue("customerId", customerId);

            for (Partners partner : partners) {
                Optional<Customer> partnerCustomer = customerDao.getById(partner.partnerId);
                if (partnerCustomer.isPresent()) {
                    locationListResponses.addAll(getPartner(req, res, partner.partnerId).data);
                }
            }
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in LocationController.getAll", e);
            return multipleDetailedLocationListResponse;
        }
        return multipleDetailedLocationListResponse;
    }

    /*
     * Get a single location. Expects the id to be provided as a path parameter. Returns a
     * status 500 error if there is an exception of any sort.
     */
    public LocationResponse get(Request req, Response res) {
        try {
            String id = req.params("id");
            Optional<Location> l = locationDao.getById(id);
            if (!l.isPresent()) {
            	//May be a Partner's
            	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
                String customerId = profile.getCustomerId();
                String customerSlug = profile.getCustomerSlug();
                Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
                
            	List<Partners> partners = partnersDao.getAllByFieldValue("customerId", customerId);
            	boolean shareData =  customer.dataSharingConsent;
            	boolean iAmAdmin = false;
    			if (customer.license != null) {
    				if (customer.license.licenseType != null) {
    					if (customer.license.licenseType == LicenseType.Master) {
    						shareData = true;
    						iAmAdmin = true;
    					}
    				}
    			}
    			if (shareData) {
                	for (int ii = 0; ii < partners.size(); ii ++ ) {
                		Optional<Customer> partnerCustomer = customerDao.getById(partners.get(ii).partnerId);
                		if (!partnerCustomer.isPresent()) {
                			continue;
                		}
                		Customer partnerCust = partnerCustomer.get();
                		if (partnerCust.dataSharingConsent || iAmAdmin) {
                    		String partnerSlug = partnerCust.slug;
                    		if (partnerSlug.equalsIgnoreCase(customerSlug)) {
                    			continue;
                    		}
                    		SlugContext.setSlug(partnerSlug);
                    		l = locationDao.getById(id);
                    		SlugContext.clearSlug();
                    		if (l.isPresent()) {
                    			//Found
                    			break;
                    		}
                		}
                	}   
                	SlugContext.setSlug(customerSlug);
    			}
            }
            return l.map(responseBuilder.applyPartially(req))
                    .orElseGet(() -> {
                        res.status(HttpStatus.SC_NOT_FOUND);
                        return responseBuilder.apply(req, null);
                    });
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error getting location", e);
            return responseBuilder.apply(req, null);
        }
    }

    /*
     * Get a single location. Expects the id to be provided as a path parameter. Returns a
     * status 500 error if there is an exception of any sort.
     */
    public LocationResponse getFromPosition(Request req, Response res) {
        try {
            String latStr = req.params("lat");
            String lonStr = req.params("lon");
            
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            
            Point pt = new Point(new Position(lon, lat));
            
            Location location = messageService.getLocationByPoint(pt);

            if (location != null) {
                res.status(HttpStatus.SC_OK);
                LocationResponse lr = responseBuilder.apply(req, location);
                Optional<LinkRelation> self = LinkRelationUtil.getByRelation("self", lr.links);
                self.ifPresent(linkRelation -> res.header(HttpHeaders.LOCATION, linkRelation.href));    
                return lr;
            } else {
                res.status(HttpStatus.SC_NO_CONTENT);                
                return responseBuilder.apply(req, null);
            }
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error getting location", e);
            return responseBuilder.apply(req, null);
        }
    }

    public LocationResponse addLocation(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "ADD_LOCATION");
        	Date now = new Date();
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId   = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer   = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
        	String userID = profile.getUserId();
            String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(now);
            String body = req.body();
            Location location = objectMapper.readValue(req.body(), Location.class);
            location.id = UUID.randomUUID().toString();
            location.customerId   = customerId;
            location.customerSlug = customerSlug;
            if (location.building == null) {
				location.building = new Building();
			}
        	if (Strings.isNullOrEmpty(location.building.originalPrePlan)) {
        		location.building.originalPrePlan = timeStamp;
        	}
        	location.building.lastReviewedOn = timeStamp;
			Optional<User> u = userDao.getById(userID);
			if(u.isPresent())
			{
				User user = u.get();
				location.building.lastReviewedBy = user.firstName + " " + user.lastName;
			}            		
        	
            Preplan pp = preplanService.preplan(customer, location.geoOutline, location.storey, location.storeyBelow);

            if(location.roofArea == null || location.roofArea == 0) {
                location.roofArea = pp.roofArea;
            }
            if(location.requiredFlow == null || location.requiredFlow == 0) {
                location.requiredFlow = pp.requiredFlow;
            }
            if (location.hydrants == null || location.hydrants.size() == 0) {
                location.hydrants = new ArrayList<>();
                for(HydrantRef hr : pp.hydrants) {
                    location.hydrants.add(hr.id);
                }
            }
            else {
            	for (String hrId : location.hydrants) {
            		if (!hydrantDao.getById(hrId).isPresent()) {
            			//If Not Present then remove this
            			location.hydrants.remove(hrId);
            		}
            	}
            }
            //location hydrants must be checked if accidently partner's hydrants are selected
            location.createdOn = now;            
            location.modifiedOn = now;
            location.createdBy = userID;
            location.modifiedBy = userID;
            locationDao.save(location);

            res.status(HttpStatus.SC_CREATED);

            LocationResponse lr = responseBuilder.apply(req, location);
            Optional<LinkRelation> self = LinkRelationUtil.getByRelation("self", lr.links);
            self.ifPresent(linkRelation -> res.header(HttpHeaders.LOCATION, linkRelation.href));
            return lr;
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating location", e);
        } finally {
        	xray.commitLog();
        }

        return new LocationResponse();
    }

    public LocationResponse updateLocation(Request req, Response res)
    {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	xray.SetCustomerInfo(req, res, "UPDATE_LOCATION");
    	Date now = new Date();
        String id = req.params("id");

        Optional<Location> l = locationDao.getById(id);
        if(l.isPresent())
        {
            Location loc = l.get();
            try
            {
            	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            	String userID = profile.getUserId();
            	String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(now);
            	
            	if (loc.building == null) {
					loc.building = new Building();
				}
            	if (Strings.isNullOrEmpty(loc.building.originalPrePlan)) {
            		loc.building.originalPrePlan = timeStamp;
            	}
            		
            	loc.building.lastReviewedOn = timeStamp;
            	loc.building.lastReviewedBy = "";
            	
                Location patchedLoc = PatchUtil.patch(req.body(), loc, Location.class);
            	if (Strings.isNullOrEmpty(patchedLoc.building.originalPrePlan)) {
            		patchedLoc.building.originalPrePlan = timeStamp;
            	}
            	patchedLoc.building.lastReviewedOn = timeStamp;            		
				Optional<User> u = userDao.getById(userID);
				if(u.isPresent())
				{
					User user = u.get();
					patchedLoc.building.lastReviewedBy = user.firstName + " " + user.lastName;
				}
            	for (String hrId : patchedLoc.hydrants) {
            		if (!hydrantDao.getById(hrId).isPresent()) {
            			//If Not Present then remove this
            			patchedLoc.hydrants.remove(hrId);
            		}
            	}
            	patchedLoc.modifiedOn = now;
            	patchedLoc.modifiedBy = userID;
            	
                locationDao.replaceById(id, patchedLoc);
                res.header("Location", LocationResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
                return responseBuilder.apply(req, patchedLoc);
            }
            catch(Exception e)
            {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return responseBuilder.apply(req, null);
            } finally {
            	xray.commitLog();    	
            }
        }
        xray.commitLog();
        res.status(HttpStatus.SC_NOT_FOUND);
        return responseBuilder.apply(req, null);
    }

    public LocationResponse deleteLocation(Request req, Response res)
    {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	xray.SetCustomerInfo(req, res, "DELETE_LOCATION");
        String id = req.params("id");
        Optional<Location> l = locationDao.getById(id);
        if(l.isPresent())
        {
        	try {
	                locationDao.deleteById(id);
	                //Untag Location ID from Message
	    			messageDao.updateAllByFieldValue("locationID", id, unset("locationID"));
	                return responseBuilder.apply(req, null);              		
	    	}
	        catch(Exception e)
	        {
	            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	            return responseBuilder.apply(req, null);
	        } finally {
	        	xray.commitLog();    	
	        }
        }
        xray.commitLog();
        res.status(HttpStatus.SC_NOT_FOUND);
        return responseBuilder.apply(req, null);
    }

    public ImageResponse getImage(Request req, Response res) {
        String locationId = req.params("id");
        String imageId    = req.params("imageId");
        Optional<Location> l = locationDao.getById(locationId);
        if(l.isPresent()) {
            Location location = l.get();
            Image image = null;
            for(Image i : location.images) {
                if(i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }
            if(image != null) {
                return ImageResponseBuilder.build(req, image);
            }
            else {
                log.error("Unable to find image with id {}", imageId);
                res.status(HttpStatus.SC_NOT_FOUND);
                return null;
            }
        }
        else {
            log.error("Unable to find location with id {}", locationId);
            res.status(HttpStatus.SC_NOT_FOUND);
            return null;
        }

    }

    public ImageResponse uploadImage(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {        	
        	xray.SetCustomerInfo(req, res, "UPLOAD_IMAGE_LOCATION");
        	
            String locationId = req.params("id");
            String customerId = ProfileUtil.getValidatedProfile(req, res).getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/Users/mike/Downloads"));
            Part filePart = req.raw().getPart("file");
            String header = filePart.getHeader("Content-Disposition");
            HeaderElement[] headerElements = BasicHeaderValueParser.parseElements(header, BasicHeaderValueParser.INSTANCE);
            String filename = null;
            for(HeaderElement he : headerElements) {
                if(he.getName().equals("form-data")) {
                    NameValuePair nvp = he.getParameterByName("filename");
                    filename = nvp != null ? nvp.getValue() : null;
                    break;
                }
            }

            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);

            String userID = profile.getUserId();
            String userName = "";

            Optional<User> u = userDao.getById(userID);
            if(u.isPresent())
            {
                User user = u.get();
                userName = user.firstName + " " + user.lastName;
            }

            Tuple2<String, String> filenameParts = ImageService.splitFilename(filename);
            if(ImageService.validFileExtension(filenameParts.v2)) {
                try (InputStream inputStream = filePart.getInputStream()) {
                    Image i = imageService.uploadImage(inputStream, customer.slug, locationId, filename, userName);
                    return ImageResponseBuilder.build(req, i);
                }
            }
            else {
                res.status(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
                return ImageResponseBuilder.build(req, null);
            }
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error uploading file", e);
        } finally {
        	xray.commitLog();
        }
        return ImageResponseBuilder.build(req, null);
    }

    public ImageResponse deleteImage(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	xray.SetCustomerInfo(req, res, "DELETE_IMAGE_LOCATION");
    	xray.commitLog();
    	
        String locationId = req.params("id");
        String imageId    = req.params("imageId");
        Optional<Location> l = locationDao.getById(locationId);
        Image imageToDelete = null;
        if(l.isPresent()) {
            Location location = l.get();
            List<Image> images = new ArrayList<>();
            for(Image i : location.images) {
                if(!i.id.equals(imageId)) {
                    images.add(i);
                }
                else {
                    imageToDelete = i;
                }
            }
            location.images = images;
            locationDao.replaceById(locationId, location);

            if(imageToDelete != null) {
                imageService.deleteImage(imageToDelete.href);
                imageService.deleteImage(imageToDelete.hrefOriginal);
                imageService.deleteImage(imageToDelete.hrefThumbnail);
            }

            return ImageResponseBuilder.build(req, null);
        }
        else {
            log.error("Unable to find location with id {}", locationId);
            res.status(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    public ImageResponse setAnnotationMetadata(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);    	
        try {
        	xray.SetCustomerInfo(req, res, "ANNOTATE_IMAGE_LOCATION");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerSlug = profile.getCustomerSlug();
            String locationId = req.params("id");
            String imageId = req.params("imageId");
            String reqBody = req.body();

            String userID = profile.getUserId();
            String userName = "";

            Optional<User> u = userDao.getById(userID);
            if(u.isPresent())
            {
                User user = u.get();
                userName = user.firstName + " " + user.lastName;
            }


            String tmpStr = reqBody.replace("{\"annotationJson\":", "");
            int idx = tmpStr.indexOf("},\"annotationSVG\":");
            tmpStr = tmpStr.replace(",\"annotationSVG\":", "");
            
            String annotationJSON = tmpStr.substring(0, idx + 1);
            String annotationSVG = tmpStr.substring(idx + 1, tmpStr.length() - 1);
            
            Image image = imageService.annotateImage(customerSlug, locationId, imageId, annotationJSON, annotationSVG, userName);
            return ImageResponseBuilder.build(req, image);
        }
        catch(Exception e) {
            log.error("Unexpected error annotating eimage", e);
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return null;
        } finally {
        	xray.commitLog();
        }
    }

    public String getAnnotationMetadata(Request req, Response res) {
        String locationId = req.params("id");
        String imageId    = req.params("imageId");
        Optional<Location> l = locationDao.getById(locationId);
        if(l.isPresent()) {
            Location location = l.get();
            Image    image    = null;
            for(Image i : location.images) {
                if(i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }
            if(image != null) {
                return image.annotationMetadata;
            }
            else {
                log.error("Unable to find image wiht id {}", imageId);
                res.status(HttpStatus.SC_NOT_FOUND);
                return null;
            }
        }
        else {
            log.error("Unable to find location wiht id {}", locationId);
            res.status(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    public ImageAnnotation getAnnotationMetadataSVG(Request req, Response res) {
        String locationId = req.params("id");
        String imageId    = req.params("imageId");
        Optional<Location> l = locationDao.getById(locationId);
        if(l.isPresent()) {
            Location location = l.get();
            Image    image    = null;
            for(Image i : location.images) {
                if(i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }
            if(image != null) {
                return new ImageAnnotation(image.annotationMetadata, image.annotationSVG);
            }
            else {
                log.error("Unable to find image wiht id {}", imageId);
                res.status(HttpStatus.SC_NOT_FOUND);
                return null;
            }
        }
        else {
            log.error("Unable to find location wiht id {}", locationId);
            res.status(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    public ArrayList<String> getTags(Request req, Response res) {
        String locationId = req.params("id");
        String imageId    = req.params("imageId");
        Optional<Location> l = locationDao.getById(locationId);
        if(l.isPresent()) {
            Location location = l.get();
            Image    image    = null;
            for(Image i : location.images) {
                if(i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }
            if(image != null) {
            	return image.tags;
            }
            else {
                log.error("Unable to find image with id {}", imageId);
                res.status(HttpStatus.SC_NOT_FOUND);
                return null;
            }
        }
        else {
            log.error("Unable to find location with id {}", locationId);
            res.status(HttpStatus.SC_NOT_FOUND);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
	public ImageResponse setTags(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "TAG_IMAGE_LOCATION");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerSlug = profile.getCustomerSlug();
            String locationId = req.params("id");
            String imageId = req.params("imageId");
            String tagsBody = req.body();
            ArrayList<String> tags = new ArrayList<String>();
            String userID = profile.getUserId();
            String userName = "";

            Optional<User> u = userDao.getById(userID);
            if(u.isPresent())
            {
                User user = u.get();
                userName = user.firstName + " " + user.lastName;
            }

            ArrayList<HashMap<String, String>> resultMap = new ArrayList<HashMap<String,String>>();
	        ObjectMapper mapperObj = new ObjectMapper();
	        resultMap = mapperObj.readValue(tagsBody, 
                    new TypeReference<ArrayList<HashMap<String,String>>>(){});
	        for(int ii = 0; ii < resultMap.size(); ii ++) {
	        	HashMap<String, String> resultHashMap = resultMap.get(ii);
	        	String val = resultHashMap.get("id");
	        	tags.add(val);
	        }
	        Image image = imageService.tagImage(customerSlug, locationId, imageId, tags, userName);
            return ImageResponseBuilder.build(req, image);
        }
        catch(Exception e) {
            log.error("Unexpected error annotating eimage", e);
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return null;
        } finally {
        	xray.commitLog();
        }
    }
    
    public Preplan preplan(Request req, Response res) {
        Preplan p;
        try {
            String locationId = req.params("id");
            Location location = locationDao.getById(locationId).orElseThrow(() -> new RuntimeException("Cannot find location object"));
            String customerId = ProfileUtil.getValidatedProfile(req, res).getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            String aaString = req.queryParamOrDefault("autoAccept", "false");
            boolean autoAccept = aaString.equalsIgnoreCase("true");
            p = preplanService.preplan(customer, location, autoAccept);
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_NOT_FOUND);
            log.error("Error preplanning location", e);
            p = new Preplan();
        }
        return p;
    }

    public ImageResponse reorderImage(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "REORDER_IMAGE_LOCATION");
            String locationId = req.params("id");
            String customerId = ProfileUtil.getValidatedProfile(req, res).getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            ArrayList<Object>   imageListReceived  = objectMapper.readValue(req.body(), new TypeReference<ArrayList<Object>>() {});
            Optional<Location> l = locationDao.getById(locationId);
            if(l.isPresent()) {
                Location location = l.get();
                List<Image> imageOrig = new ArrayList<Image>(location.images);
                List<Image> imageDest = new ArrayList<Image>(location.images);
                imageDest.clear();                
                for (int ii = 0; ii < imageListReceived.size(); ii ++) {
                	int found = -1;
                	HashMap<String, String> imageObject = (HashMap<String, String>)imageListReceived.get(ii);
                	for (int jj = 0; jj < imageOrig.size(); jj ++) {
                		Image image = imageOrig.get(jj);
                		String imageID = imageObject.get("id");
                		if (image.id.equals(imageID)) {
                			found = jj;
                			break;
                		}
                	}
                	imageDest.add(imageOrig.get(found));
                }
                
                //Now re-arrange this list according to the incoming list.
                location.images = imageDest;
                locationDao.replaceById(locationId, location);
                return ImageResponseBuilder.build(req, null);
            }
        }
        catch(Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error uploading file", e);
        } finally {
        	xray.commitLog();
        }
        return ImageResponseBuilder.build(req, null);
    }
    
    private static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterable.iterator(),
                Spliterator.IMMUTABLE
            ),
            false
        );
    }
    
    public LocationUploadResponse uploadPrePlan(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	LocationUploadResponse returnResponse = new LocationUploadResponse();
    	try {
    		xray.SetCustomerInfo(req, res, "UPLOAD_PREPLAN");
        	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            String userId = profile.getUserId();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload();

            FileItemIterator iter = upload.getItemIterator(req.raw());
            if (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream inputStream = item.openStream();
                preplanService.uploadPrePlan(inputStream, customer, userId, objectMapper, returnResponse);
                try {
                	inputStream.close();
                } catch(Exception i) {
                	
                }
            }           
            return returnResponse;
        } catch (Exception e) {
            log.error("Error uploading preplan data file", e);
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnResponse.successFlag = 1;
            returnResponse.msg = "Exception occured with message:" + e.toString();
            return returnResponse;
        } finally {
        	xray.commitLog();
        }
    }
}
