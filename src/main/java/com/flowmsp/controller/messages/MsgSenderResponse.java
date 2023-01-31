package com.flowmsp.controller.messages;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.domain.Message;
import com.google.common.collect.Lists;
import org.jooq.lambda.function.Function2;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;

public class MsgSenderResponse extends MsgSender {
    public static MsgSenderResponse build(Request req, MsgSender msgReceiver) {
    	MsgSenderResponse resp = new MsgSenderResponse();
        if(msgReceiver != null) {
            resp.customerID        = msgReceiver.customerID;
            resp.text          = msgReceiver.text;
            resp.textRaw          = msgReceiver.textRaw;
            resp.address		= msgReceiver.address;
            resp.status			= msgReceiver.status;
            resp.type			= msgReceiver.type;
        }
        return resp;
    }

    public static final Function2<Request, MsgSender, MsgSenderResponse> builder() {
        return MsgSenderResponse::build;
    }
}
