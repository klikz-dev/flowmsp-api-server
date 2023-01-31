package com.flowmsp.service.pubsub;

import java.util.List;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.controller.messages.MsgSender;
import com.flowmsp.service.ServerSendEventHandler;
import com.flowmsp.service.Message.MessageService;

public class googlepulltimer extends TimerTask {
    private static final Logger log = LoggerFactory.getLogger(googlepulltimer.class);
    private static Boolean isExecuting;
    private final MessageService msgService;
    private final ObjectMapper   objectMapper;
    
    public googlepulltimer(ObjectMapper objectMapper, MessageService msgService) {
        this.objectMapper   = objectMapper;
        this.msgService		= msgService;
        googlepulltimer.isExecuting = false;
    }
	
    @Override
    public void run() {
	//1) Check unread Messages
	//2) In case there are unread message then send them to emitter
        //Send Message Instantly
	try {
	    if (isExecuting) {
		log.info("Google pull timer still executing, hence skipping");
		return;
	    }			

	    isExecuting = true;
	    log.info("Google pull timer entered");

	    List<MsgSender> unreadMsg =  msgService.readAndStoreUnreadMessages();

	    for (int ii = 0; ii < unreadMsg.size(); ii ++) {
		MsgSender msgClient = unreadMsg.get(ii);
		ServerSendEventHandler.GetMyInstance().SendData(msgClient.customerID, objectMapper.writeValueAsString(msgClient), msgClient.sequence);			
	    }

	    log.info("Google pull timer exited");
	} catch (Exception ex) {
	    log.error("Error in transmitting unread emails", ex);
	} finally {
	    isExecuting = false;
	}
    }
}

