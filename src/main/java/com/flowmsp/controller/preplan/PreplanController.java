package com.flowmsp.controller.preplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.hydrant.HydrantRef;
import com.flowmsp.service.preplan.Preplan;
import com.flowmsp.service.preplan.PreplanPolyStorey;
import com.flowmsp.service.preplan.PreplanService;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.mongodb.client.model.geojson.Polygon;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class PreplanController {
    private static final Logger log = LoggerFactory.getLogger(PreplanController.class);

    private final CustomerDao    customerDao;
    private final PreplanService preplanService;
    private final ObjectMapper   objectMapper;

    public PreplanController(CustomerDao customerDao, PreplanService preplanService, ObjectMapper objectMapper) {
        this.customerDao    = customerDao;
        this.preplanService = preplanService;
        this.objectMapper   = objectMapper;
    }

    public Preplan preplan(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId   = profile.getCustomerId();
            String customerSlug = profile.getCustomerSlug();
            Customer customer   = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            String bodyData = req.body();
            Polygon polygon = null;
            int storey = 0, storeyBelow = 0;
            PreplanPolyStorey polyStorey = null;
            try {
                polyStorey = objectMapper.readValue(bodyData, PreplanPolyStorey.class);            	
            } catch (Exception ex) {
            	polygon = objectMapper.readValue(bodyData, Polygon.class);
            	polyStorey = new PreplanPolyStorey(polygon);
            }
            
        	polygon = polyStorey.locationCoords;
        	storey = polyStorey.storey;
        	storeyBelow = polyStorey.storeyBelow;
        	
            if (storey < 0) {
            	storey = 0;
            }
            if (storeyBelow < 0) {
            	storeyBelow = 0;
            }
            if (storey <= 0 && storeyBelow <= 0) {
            	storey = 1;
            }
            
            Preplan preplan = preplanService.preplan(customer, polygon, storey, storeyBelow);
            return buildHrefs(preplan, req, customerSlug);
        }
        catch(Exception e) {
            log.error("Error preplanning polygon", e);
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return new Preplan();
        }
    }

    private Preplan buildHrefs(Preplan prelan, Request req, String slug) {
        String protocol     = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        for(HydrantRef hr : prelan.hydrants) {
            hr.href = String.format("%s/api/%s/hydrant/%s", protocolHost, slug, hr.id);
        }

        return prelan;
    }
}


