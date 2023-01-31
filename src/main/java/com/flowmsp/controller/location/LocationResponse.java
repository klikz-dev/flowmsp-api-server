package com.flowmsp.controller.location;

import com.flowmsp.SlugContext;
import com.flowmsp.controller.LinkRelation;
import com.flowmsp.controller.hydrant.HydrantResponse;
import com.flowmsp.domain.*;
import com.flowmsp.domain.hydrant.HydrantRef;
import com.flowmsp.domain.location.Building;
import com.flowmsp.domain.location.Image;
import com.flowmsp.domain.location.Location;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mongodb.client.model.geojson.Polygon;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LocationResponse  {
    public List<LinkRelation> links = null;

    public String           id;
    public String           customerId;
    public String           customerSlug;
    public String           name;
    public int				storey;
    public int				storeyBelow;
    public String			lotNumber;
    public Long             roofArea;
    public Long             requiredFlow;
    public Polygon          geoOutline;
    public Address          address;
    public List<HydrantRef> hydrants = new ArrayList<>();
    public List<Image>      images   = new ArrayList<>();
    public Building building;
    public int				imageLength;
    public static LocationResponse build(Request req, Location location) {
        LocationResponse resp = new LocationResponse();        
        if(location != null) {
            resp.id            = location.id;
            resp.customerId    = location.customerId;
            if (Strings.isNullOrEmpty(location.customerSlug)) {
            	location.customerSlug = SlugContext.getpartnerSlug().get();
            }
            resp.customerSlug  = location.customerSlug;
            resp.name          = location.name;
            resp.storey		   = location.storey;
            resp.storeyBelow   = location.storeyBelow;
            resp.lotNumber	   = location.lotNumber;
            resp.roofArea      = location.roofArea;
            resp.requiredFlow  = location.requiredFlow;
            resp.geoOutline    = location.geoOutline;
            resp.address       = location.address;
            resp.hydrants      = location.hydrants
                                         .stream()
                                         .map(id -> new HydrantRef(id, HydrantResponse.selfLink(req, location.customerSlug, id)))
                                         .collect(Collectors.toList());
            resp.images        = location.images;
            resp.imageLength   = location.images.size();
            resp.building = location.building;
            resp.buildLinks(req);
        }
        return resp;
    }
    
    public static LocationResponse buildMini(Request req, Location location) {
        LocationResponse resp = new LocationResponse();
        if(location != null) {
            resp.id            = location.id;
            resp.customerId    = location.customerId;
            if (Strings.isNullOrEmpty(location.customerSlug)) {
            	location.customerSlug = SlugContext.getpartnerSlug().get();
            }
            resp.customerSlug  = location.customerSlug;
            resp.name          = location.name;
            resp.storey		   = location.storey;
            resp.storeyBelow   = location.storeyBelow;
            resp.lotNumber	   = location.lotNumber;
            resp.roofArea      = location.roofArea;
            resp.requiredFlow  = location.requiredFlow;
            resp.geoOutline    = location.geoOutline;
            resp.address       = location.address;
            resp.hydrants      = location.hydrants
                                         .stream()
                                         .map(id -> new HydrantRef(id, HydrantResponse.selfLink(req, location.customerSlug, id)))
                                         .collect(Collectors.toList());
            resp.imageLength   = location.images.size();
            resp.building = location.building;
            resp.buildLinks(req);
        }
        return resp;
    }

    /*
     * Build the links that are specific to the location response
     */
    public static String selfLink(Request req, String slug, String id) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        return String.format("%s/api/%s/location/%s", protocolHost, slug, id);
    }

    private void buildLinks(Request req) {
        links = Lists.newArrayList();

        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        // Self link
        links.add(new LinkRelation("self", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/location/%s", protocolHost, customerSlug, id)));

        // Customer link
        links.add(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/customer/%s", protocolHost, customerSlug, customerId)));

        // Preplan
        links.add(new LinkRelation("preplan", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/location/%s/preplan", protocolHost, customerSlug, id)));
    }
}
