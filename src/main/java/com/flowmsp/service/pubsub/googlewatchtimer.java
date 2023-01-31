package com.flowmsp.service.pubsub;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class googlewatchtimer extends TimerTask {
	private static final Logger log = LoggerFactory.getLogger(googlewatchtimer.class);
	private static Boolean isExecuting;
    
	public googlewatchtimer() {
        this.isExecuting = false;
    }
	
	@Override
	public void run() {
		//1) Check unread Messages
		//2) In case there are unread message then send them to emitter
        //Send Message Instantly
		try {
			if (isExecuting) {
				log.info("Watch timer still executing hence skipping");
				return;
			}
			log.info("Watch timer entered");
			isExecuting = true;
			googlepubsub.GetMyInstance().WatchExpired();
			log.info("Watch timer exited");
			
		} catch (Exception ex) {
			log.error("Error in putting watch", ex);
		} finally {
			isExecuting = false;
		}
	}
}
