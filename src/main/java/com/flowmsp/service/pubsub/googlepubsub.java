package com.flowmsp.service.pubsub;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class googlepubsub {
	private static googlepubsub myInstance = null;
	private static final Logger log = LoggerFactory.getLogger(googlepubsub.class);
	private final googlecredentials credential;
	public googleauthorization authorization;
	public googlewatcher watcher;
	public googlemail mail;
	
	private googlepubsub(googlecredentials credential) {
		this.credential = credential;	
	}
	
	public static googlepubsub GetMyInstance(googlecredentials credential) {
		if (myInstance == null) {
			myInstance = new googlepubsub(credential);			
		}
		return myInstance;
	}
	
	public static googlepubsub GetMyInstance() {				
		return myInstance;
	}
	
	public void Initialize() {
		log.info("googlepubsub intialization starts");
		this.authorization = new googleauthorization(credential);
		this.authorization.RefreshAccessToken();		
		this.watcher = new googlewatcher(this.authorization.accesstoken.topicName);		
		this.mail = new googlemail();
		log.info("googlepubsub intialization ends");
	}
	
	public int WatchExpired() {
		log.info("googlepubsub putting watch");
		return this.watcher.PutWatch();
	}
	
	public List<googlemailresult> GetUnreadEmails() {
		log.info("googlepubsub.GetUnreadEmails: fetching all unread emails");
		return this.mail.GetUnreadEmails();
	}
	
	public List<googlemailresult> GetEmailsHistory(String startHistoryId) {
		log.info("in GetEmailsHistory. googlepubsub fetching all emails of history:" + startHistoryId);
		return this.mail.GetMessageAddedEmails(startHistoryId);
	}
}
