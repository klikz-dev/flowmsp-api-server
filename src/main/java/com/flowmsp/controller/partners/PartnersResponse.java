package com.flowmsp.controller.partners;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.partners.Partners;

import spark.Request;

import java.util.List;

public class PartnersResponse extends Partners {    
	public List<LinkRelation> links = null;

    /*
     * Function that will actually create the PartnersResponse from a partners
     */
    public static PartnersResponse build(Request req, Partners partner) {
    	PartnersResponse resp = new PartnersResponse();
        if(partner != null) {
            resp.id = partner.id;
            resp.customerId    = partner.customerId;
            resp.partnerId      = partner.partnerId;
        }
        return resp;
    }
}
