package com.flowmsp.controller.debugPanel;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.debugpanel.debugInfo;
import org.jooq.lambda.function.Function2;
import spark.Request;

import java.util.List;

public class debugPanelResponse extends debugInfo {
    public List<LinkRelation> links = null;

    /*
     * Function that will actually create the debugPanelResponse from a debugInfo
     */
    public static debugPanelResponse build(Request req, debugInfo xray) {
        debugPanelResponse resp = new debugPanelResponse();
        if(xray != null) {
            resp.id					= xray.id;
            resp.Source				= xray.Source;
            resp.TimeStamp			= xray.TimeStamp;
            resp.ErrorFlag			= xray.ErrorFlag;
            resp.ErrorDescription	= xray.ErrorDescription;
            resp.Details			= xray.Details;
        }
        return resp;
    }

    public static final Function2<Request, debugInfo, debugPanelResponse> builder() {
        return debugPanelResponse::build;
    }    
 }
