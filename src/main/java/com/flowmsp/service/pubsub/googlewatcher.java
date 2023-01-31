package com.flowmsp.service.pubsub;

import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minidev.json.JSONObject;

public class googlewatcher {
	private static final Logger log = LoggerFactory.getLogger(googlewatcher.class);
	private String topicName;
	public googlehistory history;
	public Date watchTimeStamp;
	
	public googlewatcher(String topicName) {
		this.topicName = topicName;
	}	
	
	public int PutWatch() {
		try {
			this.history = new googlehistory();		
			String targetURL = "https://www.googleapis.com/gmail/v1/users/me/watch";
			String urlParameters = "{" +
					 "\"topicName\":" + "\"" + topicName  + "\"," +
					"\"labelIds\":[\"INBOX\"]" +
				 	"}";
			JSONObject jb = pubsubhttp.GetMyInstance().PerformPOST(targetURL, urlParameters);
			if (jb != null) {
				this.history.historyId = jb.get("historyId").toString();
				Calendar calendar = Calendar.getInstance();
				this.watchTimeStamp = calendar.getTime();			
				this.history.historyTimeStamp = calendar.getTime();
				log.info("Successfully put watch on topic:" + topicName + " " + this.history.historyTimeStamp);
				return 0;
			}
			return -1;			
		} catch (Exception e) {
			log.error("Error Putting Watch", e);
			return -2;
		}		
	}
}
