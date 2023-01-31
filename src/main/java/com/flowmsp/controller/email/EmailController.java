package com.flowmsp.controller.email;

import java.util.List;
import java.util.UUID;

import com.flowmsp.service.FCMService;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.messages.MsgReceiverController;
import com.flowmsp.controller.messages.MsgReceiverResponse;
import com.flowmsp.controller.messages.MsgSender;
import com.flowmsp.db.MessageDao;
import com.flowmsp.domain.Message;
import com.flowmsp.service.ServerSendEventHandler;
import com.flowmsp.service.Message.MessageResult;
import com.flowmsp.service.Message.MessageService;
import com.mongodb.client.model.Filters;

import spark.Request;
import spark.Response;

public class EmailController {
    private static final Logger log = LoggerFactory.getLogger(MsgReceiverController.class);
    private final MessageDao    msgReceiverDao;
    private final MessageService msgService;
    private final FCMService fcmService;
    private final ObjectMapper   objectMapper;

    public EmailController(MessageDao msgReceiverDao, ObjectMapper objectMapper, MessageService msgService, FCMService fcmService) {
	this.msgReceiverDao = msgReceiverDao;
	this.objectMapper   = objectMapper;
	this.msgService		= msgService;
	this.fcmService		= fcmService;
    }
	
    public MsgReceiverResponse addMsg(Request req, Response res) {
	try {
	    log.info("in EmailController.addMsg");

	    res.status(HttpStatus.SC_NO_CONTENT);
	    List<MessageResult> msgResult = msgService.ParsePushNotification(req);
	    //List<MessageResult> msgResult = msgService.test();

	    log.info(msgResult.size() + " messages returned from msgService.ParsePushNotification");
	    
	    for (int ii = 0; ii < msgResult.size(); ii ++) {
		if (msgResult.get(ii).errorFlag != 0) {
		    continue;
		}
		
		Message msg = new Message();
		msg.id = UUID.randomUUID().toString();
		msg.sequence = System.currentTimeMillis();
		msg.textRaw = msgResult.get(ii).messageRaw;
		msg.customerId = msgResult.get(ii).customer.id;
		msg.customerSlug = msgResult.get(ii).customer.slug;
		msg.address = msgResult.get(ii).messageAddress;
		msg.status = "";
		msg.text = msgResult.get(ii).messageRefined;
		msg.type = msgResult.get(ii).messageType;
		msg.locationID = msgResult.get(ii).messageLocationID;
		msg.latLon = msgResult.get(ii).messageLatLon;
		msg.messageID = msgResult.get(ii).messageID;
		msg.units = msgResult.get(ii).units;
		msg.incidentID = msgResult.get(ii).incidentID;
		msg.source = "email";

		SlugContext.setSlug(msgResult.get(ii).customer.slug);
		//if message is already there then don't push it again
		List<Message> msgList = msgReceiverDao.getAllByFilter(Filters.and(Filters.eq("messageID", msgResult.get(ii).messageID), Filters.eq("source", "email")));
		if (msgList.size() > 0) {
		    //Already Added
		    SlugContext.clearSlug();
		    log.info(msg.customerSlug + " Already Added Email Message:" + msgResult.get(ii).messageID);
		    continue;
		}

		log.info(msg.customerSlug + " Saving Email Message:" + msgResult.get(ii).messageID);
		msgReceiverDao.save(msg);
		SlugContext.clearSlug();
		
		//Send Message Instantly
		MsgSender msgClient = new MsgSender();    		
		msgClient.id = msg.id;
		msgClient.sequence = msg.sequence;
		msgClient.address = msg.address;
		msgClient.text = msg.text;
		msgClient.textRaw = msg.textRaw;
		msgClient.customerID = msg.customerId;
		msgClient.type = msg.type;
		msgClient.locationID = msg.locationID;
		msgClient.latLon = msg.latLon;
		msgClient.status = msg.status;
		msgClient.units = msg.units;
		msgClient.incidentID = msg.incidentID;
		ServerSendEventHandler.GetMyInstance().SendData(msg.customerId, this.objectMapper.writeValueAsString(msgClient), msg.sequence);
		fcmService.sendDispatchPushNotification(msgResult.get(ii).customer.slug, msgResult.get(ii).customer.id, msgClient);
		}
	    res.status(HttpStatus.SC_NO_CONTENT);
	}
	catch(Exception e) {
	    res.status(HttpStatus.SC_INTERNAL_SERVER_ERROR);
	    log.error("Error adding SMS", e);
	    return MsgReceiverResponse.builder().apply(req, null);
	}
	finally {
	    SlugContext.clearSlug();
	}
	return MsgReceiverResponse.builder().apply(req, null);
    }    
}
