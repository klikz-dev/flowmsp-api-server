package com.flowmsp.controller.hydrant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.LinkRelation;
import com.flowmsp.controller.LinkRelationUtil;
import com.flowmsp.controller.location.LocationUploadResponse;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.db.HydrantDao;
import com.flowmsp.db.PartnersDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.partners.Partners;
import com.flowmsp.service.HydrantDeleteResult;
import com.flowmsp.service.HydrantService;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.patch.PatchNotAllowedException;
import com.flowmsp.service.patch.PatchUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.google.common.net.HttpHeaders;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class HydrantController {
    private static final Logger log = LoggerFactory.getLogger(HydrantController.class);

    private final HydrantDao hydrantDao;
    private final CustomerDao customerDao;
    private final PartnersDao partnersDao;
    private final HydrantService hydrantService;
    private final ObjectMapper objectMapper;
    private final DebugInfoDao debugInfoDao;
    
    public HydrantController(HydrantDao hydrantDao, CustomerDao customerDao, PartnersDao partnersDao, HydrantService hydrantService, ObjectMapper objectMapper, DebugInfoDao debugInfoDao) {
        this.hydrantDao = hydrantDao;
        this.customerDao = customerDao;
        this.partnersDao = partnersDao;
        this.hydrantService = hydrantService;
        this.objectMapper = objectMapper;
        this.debugInfoDao = debugInfoDao;
    }

    /*
     * Gets all the hydrants for the customer.
     *
     * Allows the following query parameters:
     * limit: limits the number of hydrants returned.
     *
     * This will return a list of hydrant responses. If no hydrants are found an empty list
     * is returned. If there is any exception thrown during processing a 500 error is returned.
     */
    
    public HydrantListResponse getAll(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            return stream(hydrantDao.getCollection().find())
                        .map(HydrantResponse.builder().applyPartially(req))
                        .collect(HydrantListResponse::new,
                        		HydrantListResponse::accept,
                        		HydrantListResponse::combine);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.getAll", e);
            return new HydrantListResponse();
        }
    }
    
    public HydrantListResponse getPartner(Request req, Response res) {
        try {
        	ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
        	String partnerId = req.params("partnerId");
            String customerId = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
    		Optional<Customer> partnerCustomer = customerDao.getById(partnerId);
    		if (!partnerCustomer.isPresent()) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in HydrantController.getPartner, Partner Not Found:" + partnerId);
                return new HydrantListResponse();    			
    		}
    		Customer partnerCust = partnerCustomer.get();
    		String partnerSlug = partnerCust.slug;
    		if (partnerSlug.equalsIgnoreCase(customerSlug)) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in HydrantController.getPartner, Trying to get own data as partner:" + partnerId);
                return new HydrantListResponse();
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
				return new HydrantListResponse();
			}
    		SlugContext.setPartnerSlug(customerSlug);
    		SlugContext.setSlug(partnerSlug);
    		
            return stream(hydrantDao.getCollection().find())
                    .map(HydrantResponse.builder().applyPartially(req))
                    .collect(HydrantListResponse::new,
                    		HydrantListResponse::accept,
                    		HydrantListResponse::combine);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.getAll", e);
            return new HydrantListResponse();
        }
    }

    public HydrantListResponse getPartner(Request req, Response res, String partnerId) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            Optional<Customer> partnerCustomer = customerDao.getById(partnerId);
            if (!partnerCustomer.isPresent()) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in HydrantController.getPartner, Partner Not Found:" + partnerId);
                return new HydrantListResponse();
            }
            Customer partnerCust = partnerCustomer.get();
            String partnerSlug = partnerCust.slug;
            if (partnerSlug.equalsIgnoreCase(customerSlug)) {
                res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                log.error("Error in HydrantController.getPartner, Trying to get own data as partner:" + partnerId);
                return new HydrantListResponse();
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
                return new HydrantListResponse();
            }
            SlugContext.setPartnerSlug(customerSlug);
            SlugContext.setSlug(partnerSlug);

            return stream(hydrantDao.getCollection().find())
                    .map(HydrantResponse.builder().applyPartially(req))
                    .collect(HydrantListResponse::new,
                            HydrantListResponse::accept,
                            HydrantListResponse::combine);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.getAll", e);
            return new HydrantListResponse();
        }
    }

    public MultipleDetailedHydrantListResponse getAllPartner(Request req, Response res) {
        List<HydrantResponse> hydrantListResponses = new ArrayList<>();
        MultipleDetailedHydrantListResponse multipleDetailedHydrantListResponse = new MultipleDetailedHydrantListResponse(hydrantListResponses);

        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Optional<Customer> customer = customerDao.getById(customerId);

            customer.ifPresent(value -> hydrantListResponses.addAll(getAll(req, res).data));

            List<Partners> partners = partnersDao.getAllByFieldValue("customerId", customerId);

            for (Partners partner : partners) {
                Optional<Customer> partnerCustomer = customerDao.getById(partner.partnerId);
                if (partnerCustomer.isPresent()) {
                    hydrantListResponses.addAll(getPartner(req, res, partner.partnerId).data);
                }
            }
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in LocationController.getAll", e);
            return multipleDetailedHydrantListResponse;
        }
        return multipleDetailedHydrantListResponse;
    }

    /*
     * Get a single hydrant. Expects the path parameter to contain the id of the hydrant. If
     * there is any exception throw during processing a 500 error is returned.
     */
    public HydrantResponse get(Request req, Response res) {
        try {
            String id = req.params("id");
            Optional<Hydrant> c = hydrantDao.getById(id);
            return c.map(HydrantResponse.builder().applyPartially(req))
                    .orElseGet(() -> {
                        res.status(HttpStatus.SC_NOT_FOUND);
                        return HydrantResponse.builder().apply(req, null);
                    });
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.get", e);
            return HydrantResponse.builder().apply(req, null);
        }
    }    
    
    /*
     * Add a hydrant to the system. If successfully created the 201 response will be returned with
     * the Location header providing the URL of the new entity. The new entity will also be
     * returned in the body of the response.
     */
    public HydrantResponse add(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "ADD_HYDRANT");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            String userId = profile.getUserId();
            Date now = new Date();
            
            Hydrant hydrant = objectMapper.readValue(req.body(), Hydrant.class);
            hydrant.id = UUID.randomUUID().toString();
            hydrant.customerId = customerId;
            hydrant.customerSlug = customerSlug;
            hydrant.determineFlowRange(customer.pinLegend);
            hydrant.createdBy = userId;
            hydrant.createdOn = now;
            hydrant.modifiedBy = userId;            
            hydrant.modifiedOn = now;
            hydrantDao.save(hydrant);
            res.status(HttpStatus.SC_CREATED);

            HydrantResponse hr = HydrantResponse.builder().apply(req, hydrant);
            Optional<LinkRelation> self = LinkRelationUtil.getByRelation("self", hr.links);
            self.ifPresent(linkRelation -> res.header(HttpHeaders.LOCATION, linkRelation.href));
            return hr;
        } catch (JsonProcessingException e) {
            res.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            log.error("Error in HydrantController.add", e);
            return HydrantResponse.builder().apply(req, null);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.add", e);
            return HydrantResponse.builder().apply(req, null);
        } finally {
        	xray.commitLog();
        }
    }

    /*
     * Update an individual hydrant. Upon success the 204 (No Content) status is returned and
     * a link to the updated entity is provided in the Location header
     */
    public HydrantResponse updateHydrant(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "UPDATE_HYDRANT");
            String id = req.params("id");
            Optional<Hydrant> h = hydrantDao.getById(id);

            if (h.isPresent()) {
                Hydrant hydrant = h.get();
                ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
                String customerId = profile.getCustomerId();
                Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
                
                String userId = profile.getUserId();
                Date now = new Date();
                
                Hydrant patchedHydrant = PatchUtil.patch(req.body(), hydrant, Hydrant.class);
                patchedHydrant.determineFlowRange(customer.pinLegend);
                patchedHydrant.modifiedBy = userId;            
                patchedHydrant.modifiedOn = now;
                hydrantDao.replaceById(hydrant.id, patchedHydrant);
                res.header("Location", HydrantResponse.selfLink(req, SlugContext.getSlug().orElse(""), id));
                return HydrantResponse.builder().apply(req, patchedHydrant);
            } else {
                res.status(HttpStatus.SC_NOT_FOUND);
                return HydrantResponse.builder().apply(req, null);
            }
        } catch (PatchNotAllowedException | NoSuchFieldException e) {
            res.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
            log.error("Error in HydrantController.update", e);
            return HydrantResponse.builder().apply(req, null);
        } catch (Exception e) {
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error in HydrantController.update", e);
            return HydrantResponse.builder().apply(req, null);
        } finally {
        	xray.commitLog();
        }
    }

    /*
     * Deletes the entity
     */
    public HydrantDeleteResponse delete(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "DELETE_HYDRANT");
            String id = req.params("id");
            Optional<HydrantDeleteResult> hdr = hydrantService.deleteByHydrantId(id);
            if (hdr.isPresent()) {
                res.status(HttpStatus.SC_OK);
                return HydrantDeleteResponse.build(req, hdr.get());
            } else {
                res.status(HttpStatus.SC_NOT_FOUND);
                return new HydrantDeleteResponse();
            }
        } catch (Exception e) {
            log.error("Error in HydrantController.delete", e);
            return new HydrantDeleteResponse();
        } finally {
        	xray.commitLog();
        }
    }
    
    /*
     * Deletes all hydrants
     */
    public HydrantDeleteResponse deleteAll(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
        try {
        	xray.SetCustomerInfo(req, res, "DELETE_HYDRANT_ALL");
            Optional<HydrantDeleteResult> hdr = hydrantService.deleteAllHydrants();
            if (hdr.isPresent()) {
                res.status(HttpStatus.SC_NO_CONTENT);
                return HydrantDeleteResponse.build(req, hdr.get());
            } else {
                res.status(HttpStatus.SC_PRECONDITION_FAILED);
                return new HydrantDeleteResponse();
            }
        } catch (Exception e) {
            log.error("Error in HydrantController.deleteAll", e);
            return new HydrantDeleteResponse();
        } finally {
        	xray.commitLog();
        }
    }    

    /*
     * Upload a file of hydrant information
     */

    public HydrantUploadResponse upload(Request req, Response res) {
    	debugPanel xray = new debugPanel(debugInfoDao);
    	HydrantUploadResponse returnResponse = new HydrantUploadResponse();
        try {
        	xray.SetCustomerInfo(req, res, "UPLOAD_HYDRANT");
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
                hydrantService.uploadHydrants(inputStream, customer, userId, returnResponse);
            }
            return returnResponse;
        } catch (Exception e) {
            log.error("Error uploading hydrant data file", e);
            res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            returnResponse.successFlag = 1;
            returnResponse.msg = "Exception occured with message:" + e.toString();
            return returnResponse;
        } finally {
        	xray.commitLog();
        }
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
}