package com.flowmsp.controller.debugPanel;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.debugpanel.debugUser;

import org.jooq.lambda.function.Function2;
import spark.Request;

import java.util.List;

public class debugPanelUserResponse extends debugUser {
    public List<LinkRelation> links = null;

    /*
     * Function that will actually create the debugPanelResponse from a debugInfo
     */
    public static debugPanelUserResponse build(Request req, debugUser usr) {
    	debugPanelUserResponse resp = new debugPanelUserResponse();
        if(usr != null) {
            resp.id					= usr.id;
            resp.email				= usr.email;
            resp.name				= usr.name;
            resp.customerName		= usr.customerName;
            resp.customerSlug 		= usr.customerSlug;
            resp.customerAddress	= usr.customerAddress;
        }
        return resp;
    }

    public static final Function2<Request, debugUser, debugPanelUserResponse> builder() {
        return debugPanelUserResponse::build;
    }    
 }
