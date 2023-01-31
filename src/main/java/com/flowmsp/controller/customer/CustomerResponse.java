package com.flowmsp.controller.customer;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.*;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.CustomerSettings;
import com.flowmsp.domain.customer.License;
import com.flowmsp.domain.flow.PinLegend;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.service.BsonUtil;
import com.google.common.collect.Lists;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;
import java.util.Map;

public class CustomerResponse {
    public List<LinkRelation> links = null;

    public String              id;
    public String              slug;
    public String              name;
    public Address             address;
    public License             license;
    public PinLegend           pinLegend;
    public CustomerSettings    settings;
    public Map<String, Object> uiConfig;
    public String			   smsNumber;
    public String			   emailGateway;
    public String			   smsFormat;
    public String			   emailFormat;
    public String			   emailSignature;
    public String			   emailSignatureLocation;
    public String			   fromContains;
    public String			   toContains;
    public String			   subjectContains;
    public String			   bodyContains;
    public String			   fromNotContains;
    public String			   toNotContains;
    public String			   subjectNotContains;
    public String			   bodyNotContains;
    public double			   boundSWLat;
    public double			   boundSWLon;
    public double			   boundNELat;
    public double			   boundNELon;
    public boolean			   dataSharingConsent;
    public boolean			   dispatchSharingConsent;
    public String			   timeZone;
    private UserRole userRole;

    /*
     * Function that will actually create the CustomerResponse from a Customer
     */
    public static CustomerResponse build(Request req, UserRole userRole, Customer customer) {
        CustomerResponse resp = new CustomerResponse();
        if(customer != null) {
            resp.id        = customer.id;
            resp.slug      = customer.slug;
            resp.name      = customer.name;
            resp.address   = customer.address;
            resp.license   = customer.license;
            resp.uiConfig  = BsonUtil.convertToObjectMap(customer.uiConfig);
            resp.pinLegend = customer.pinLegend;
            resp.settings  = customer.settings;
            resp.smsNumber = customer.smsNumber;
            resp.emailGateway = customer.emailGateway;
            resp.smsFormat = customer.smsFormat;
            resp.emailFormat = customer.emailFormat;
            resp.emailSignature = customer.emailSignature;
            resp.emailSignatureLocation = customer.emailSignatureLocation;
            resp.fromContains = customer.fromContains;
            resp.toContains = customer.toContains;
            resp.subjectContains = customer.subjectContains;
            resp.bodyContains = customer.bodyContains;
            resp.fromNotContains = customer.fromNotContains;
            resp.toNotContains = customer.toNotContains;
            resp.subjectNotContains = customer.subjectNotContains;
            resp.bodyNotContains = customer.bodyNotContains;

            resp.boundSWLat = customer.boundSWLat;
            resp.boundSWLon = customer.boundSWLon;
            resp.boundNELat = customer.boundNELat;
            resp.boundNELon = customer.boundNELon;
            resp.dataSharingConsent = customer.dataSharingConsent;
            resp.dispatchSharingConsent = customer.dispatchSharingConsent;
            resp.timeZone = customer.timeZone;
            resp.userRole  = userRole;            
            resp.buildLinks(req);
        }
        return resp;
    }

    /*
     * Build the links that are specific to the customer response
     */
    public static String selfLink(Request req, String slug, String id) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        return String.format("%s/api/%s/customer/%s", protocolHost, slug, id);
    }

    private void buildLinks(Request req) {
        links = Lists.newArrayList();

        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        // Self link
        links.add(new LinkRelation("self", HttpConstants.HTTP_METHOD.GET, CustomerResponse.selfLink(req, slug, id)));

        // Modify customer link
        if(userRole == UserRole.ADMIN) {
            links.add(new LinkRelation("customerUpdate", HttpConstants.HTTP_METHOD.PATCH, CustomerResponse.selfLink(req, slug, id)));
        }
        
        // All users
        links.add(new LinkRelation("users", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/user", protocolHost, slug)));

        // All hydrants
        links.add(new LinkRelation("hydrants", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/hydrant", protocolHost, slug)));

        // All locations
        links.add(new LinkRelation("locations", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/location", protocolHost, slug)));

        // Preplan
        links.add(new LinkRelation("preplan", HttpConstants.HTTP_METHOD.POST, String.format("%s/api/%s/preplan", protocolHost, slug)));
        
        // partners
        links.add(new LinkRelation("partners", HttpConstants.HTTP_METHOD.POST, String.format("%s/api/%s/partners/%s", protocolHost, slug, id)));        

    }
}
