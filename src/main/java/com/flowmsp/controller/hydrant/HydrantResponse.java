package com.flowmsp.controller.hydrant;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.hydrant.Hydrant;
import com.google.common.collect.Lists;
import org.jooq.lambda.function.Function2;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;

public class HydrantResponse extends Hydrant {
    public List<LinkRelation> links = null;

    /*
     * Function that will actually create the HydrantResponse from a Hydrant
     */
    public static HydrantResponse build(Request req, Hydrant hydrant) {
        HydrantResponse resp = new HydrantResponse();
        if(hydrant != null) {
            resp.id = hydrant.id;
            resp.customerId    = hydrant.customerId;
            resp.customerSlug  = hydrant.customerSlug;
            resp.latLon        = hydrant.latLon;
            resp.flow          = hydrant.flow;
            resp.size          = hydrant.size;
            resp.streetAddress = hydrant.streetAddress;
            resp.inService     = hydrant.inService;
            resp.dryHydrant	   = hydrant.dryHydrant;
            resp.notes         = hydrant.notes;
            resp.flowRange     = hydrant.flowRange;
            resp.outServiceDate = hydrant.outServiceDate;
            resp.buildLinks(req);
        }
        return resp;
    }

    public static final Function2<Request, Hydrant, HydrantResponse> builder() {
        return HydrantResponse::build;
    }

    private void buildLinks(Request req) {
        links = Lists.newArrayList();

        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        // Self link
        links.add(new LinkRelation("self", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/hydrant/%s", protocolHost, customerSlug, id)));

        // Customer link
        links.add(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/customer/%s", protocolHost, customerSlug, customerId)));
    }

    /*
     * Build the links that are specific to the hydrant response
     */
    public static String selfLink(Request req, String slug, String id) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        return String.format("%s/api/%s/hydrant/%s", protocolHost, slug, id);
    }

}
