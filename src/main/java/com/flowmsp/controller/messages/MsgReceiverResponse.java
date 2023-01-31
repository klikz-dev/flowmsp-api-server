package com.flowmsp.controller.messages;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.Message;
import com.google.common.collect.Lists;
import org.jooq.lambda.function.Function2;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;

public class MsgReceiverResponse extends Message {
    public List<LinkRelation> links = null;

    /*
     * Function that will actually create the HydrantResponse from a Hydrant
     */
    public static MsgReceiverResponse build(Request req, Message msgReceiver) {
    	MsgReceiverResponse resp = new MsgReceiverResponse();
        if(msgReceiver != null) {
            resp.id = msgReceiver.id;
            resp.customerId    = msgReceiver.customerId;
            resp.locationID		= msgReceiver.locationID;
            resp.latLon			= msgReceiver.latLon;
            resp.textRaw        = msgReceiver.textRaw;
            resp.text          = msgReceiver.text;
            resp.address		= msgReceiver.address;
            resp.status			= msgReceiver.status;
            resp.type			= msgReceiver.type;
            resp.buildLinks(req);
        }
        return resp;
    }

    public static final Function2<Request, Message, MsgReceiverResponse> builder() {
        return MsgReceiverResponse::build;
    }

    private void buildLinks(Request req) {
        links = Lists.newArrayList();

        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());
        // Customer link
        links.add(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/customer/%s", protocolHost, customerSlug, customerId)));
    }

    /*
     * Build the links that are specific to the hydrant response
     */
    public static String selfLink(Request req, String slug, String id) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        return String.format("%s/api/%s/MsgReceiver/%s", protocolHost, slug, id);
    }

}
