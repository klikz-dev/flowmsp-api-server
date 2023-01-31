package com.flowmsp.service.pubsub;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import java.util.HashMap;

public class googlemail {
    private static final Logger log = LoggerFactory.getLogger(googlemail.class);
    private static final String MAX_UNREAD_TO_RETURN = System.getenv("MAX_UNREAD_TO_RETURN");
    private static final int max_unread_to_return = (MAX_UNREAD_TO_RETURN == null ? 10 : Integer.parseInt(MAX_UNREAD_TO_RETURN));
    private static final HashMap<String, String> labels = new HashMap<String, String>();

    public googlemail() {
	log.info("loading labels");
	var url = "https://www.googleapis.com/gmail/v1/users/me/labels";
	JSONObject jb = pubsubhttp.GetMyInstance().PerfromGET(url);
	JSONArray lab = (JSONArray) jb.get("labels");
	
	for (int i = 0; i < lab.size(); i++) {
	    var id = ((JSONObject) lab.get(i)).get("id").toString();
	    var name = ((JSONObject) lab.get(i)).get("name").toString();
	    log.info("name=" + name + " id=" + id);
	    labels.put(name, id);
	}
    }

    private List<String> GetMessageAdded(String startHistoryId) {
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/history?startHistoryId=" + startHistoryId  + 
	    "&historyTypes=messageAdded";
	List<String> unreadMessages = new ArrayList<String>();
	
	//log.info("Fetching GetMessageAdded with Start HistoryID:" + startHistoryId);
	JSONObject jb = pubsubhttp.GetMyInstance().PerfromGET(targetURL);		

	try {
	    // Create connection
	    JSONArray jsonHistory = (JSONArray) jb.get("history");			
	    if (jsonHistory == null) {
		// log.info("No History Found After:" + startHistoryId);
		return unreadMessages;
	    }
	    for (int i = 0; i < jsonHistory.size(); i++) {
		JSONArray jsonMessageAdded = (JSONArray) ((JSONObject)jsonHistory.get(i)).get("messagesAdded");
		if (jsonMessageAdded == null) {
		    continue;
		}
		for (int j = 0; j < jsonMessageAdded.size(); j++) {
		    String messageID = ((JSONObject)((JSONObject)jsonMessageAdded.get(j)).get("message")).get("id").toString();
		    if (unreadMessages.contains(messageID)) {
			continue;
		    }
		    log.info("Found Message:" + messageID + " On HistoryID:" + startHistoryId);
		    unreadMessages.add(messageID);
		}
	    }
	    return unreadMessages;
	} catch (Exception e) {
	    log.error("Error GetMessageAdded:", e);
	    return unreadMessages;
	}
    }
    
    public List<googlemailresult> GetMessageAddedEmails(String startHistoryId) {
	log.info("Fetching GetMessageAddedEmails with Start HistoryID:" + startHistoryId);
	List<String> unreadMessages = GetMessageAdded(startHistoryId);
	List<googlemailresult> emails = new ArrayList<googlemailresult>();
	for (int ii = 0; ii < unreadMessages.size(); ii ++) {
	    emails.add(GetEmail(unreadMessages.get(ii)));
	}
	return emails;
    }
    
    public List<googlemailresult> GetUnreadEmails() {
	List<String> unreadMessages = GetUnreadEmailsMessageID();
	List<googlemailresult> emails = new ArrayList<googlemailresult>();

	for (int ii = 0; ii < unreadMessages.size(); ii ++) {
	    emails.add(GetEmail(unreadMessages.get(ii)));
	}
	return emails;
    }
    
    private List<String> GetUnreadEmailsMessageID() {
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/messages?q=label:unread%20label:INBOX";
	List<String> unreadMessages = new ArrayList<String>();
	JSONObject jb = pubsubhttp.GetMyInstance().PerfromGET(targetURL);		
	try {
	    // Create connection
	    JSONArray jsonMessages = (JSONArray) jb.get("messages");
	    if (jsonMessages == null) {
		log.info("No New Unread Message");
		return unreadMessages;
	    }
	    int mailKount = jsonMessages.size();

	    // this used to be hard-coded to 10 with the following comment
	    //Maximum 10 unread mails should be read otherwise the system can hang
	    if (mailKount > max_unread_to_return) {
		mailKount = max_unread_to_return;
	    }
	    log.info("Got " + jsonMessages.size() + " Unread Emails and Reading:" + mailKount);
	    
	    for (int i = 0; i < mailKount; i++) { 
		unreadMessages.add(((JSONObject)jsonMessages.get(i)).get("id").toString());
	    }

	    return unreadMessages;
	    
	} catch (Exception e) {
	    log.error("Error GetUreadEmailsMessageID:", e);
	    return unreadMessages;
	}
    }
	
    public googlemailresult GetEmail(String messageID) {
	googlemailresult mail = new googlemailresult();
	mail.messageID = "-1"; // No Message		
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/messages/" + messageID;
	JSONObject jb = pubsubhttp.GetMyInstance().PerfromGET(targetURL);
	String dataPlain = "";
	String dataOrig = "";
	String dataCoded = "";
	try {
	    JSONParser jParser = new JSONParser();
	    mail.messageID = jb.get("id").toString();
	    try {				
		JSONObject jbPayload = (JSONObject) jParser.parse(String.valueOf(jb.get("payload")));
		JSONArray jpParts = (JSONArray) jParser.parse(String.valueOf(jbPayload.get("parts")));
		if (jpParts == null) {
		    JSONObject jbBody = (JSONObject) jParser.parse(String.valueOf(jbPayload.get("body")));
		    dataCoded = String.valueOf(jbBody.get("data"));
		} else {
		    JSONObject jbFirstPart = (JSONObject)jpParts.get(0);
		    JSONObject jbBody = (JSONObject) jParser.parse(String.valueOf(jbFirstPart.get("body")));
		    dataCoded = String.valueOf(jbBody.get("data"));
		}
		dataOrig = new String(Base64.getUrlDecoder().decode(dataCoded));
		//HTML tags are present in some of the emails
		//In case it is plain text then this also will return plain text
		dataPlain = Jsoup.parse(dataOrig).text();
	    } catch (Exception ex) {
		log.debug("Exception while reading Message Body:" + ex.toString());
		dataPlain = jb.get("snippet").toString(); //Atleast we have got the snippet
	    }
	    mail.Body = dataPlain;
	    mail.BodyOrig = dataOrig;
	    mail.labelIds = (List<String>) jb.get("labelIds");			
	    mail.historyID = jb.get("historyId").toString();
	    JSONObject jbPayload1 = (JSONObject) jParser.parse(String.valueOf(jb.get("payload")));
	    JSONArray arrTemp = (JSONArray) jParser.parse(String.valueOf(jbPayload1.get("headers")));
	    mail.From = "";
	    for (int ii = 0; ii < arrTemp.size(); ii ++) {
		JSONObject jsTemp = (JSONObject) arrTemp.get(ii);
		if (jsTemp.get("name").toString().equalsIgnoreCase("from")) {
		    mail.From = jsTemp.get("value").toString();
		    break;
		}
	    }
	    mail.Subject = "";
	    for (int ii = 0; ii < arrTemp.size(); ii ++) {
		JSONObject jsTemp = (JSONObject) arrTemp.get(ii);
		if (jsTemp.get("name").toString().equalsIgnoreCase("subject")) {
		    mail.Subject = jsTemp.get("value").toString();
		    break;
		}
	    }
	    
	    mail.To = "";
	    for (int ii = 0; ii < arrTemp.size(); ii ++) {
		JSONObject jsTemp = (JSONObject) arrTemp.get(ii);
		if (jsTemp.get("name").toString().equalsIgnoreCase("to")) {
		    mail.To = jsTemp.get("value").toString();
		    break;
		}
	    }
	    
	    // if we didn't find a To header, try Delivered-To
	    if (mail.To.length() == 0) {
		for (int ii = 0; ii < arrTemp.size(); ii ++) {
		    JSONObject jsTemp = (JSONObject) arrTemp.get(ii);
		    if (jsTemp.get("name").toString().equalsIgnoreCase("Delivered-To")) {
			mail.To = jsTemp.get("value").toString();
		    }
		}
	    }

	    log.info("Successfully read message:" + messageID);
	    return mail;
	} catch (Exception e) {
	    log.error("Error GetEmail:" + messageID, e);
	    return mail;
	}
    }
	
    public boolean MarkEmailAsRead(String messageID) {
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/messages/" + messageID + "/modify";
	String urlParameters = "{" +
	    "\"addLabelIds\":[]," +
	    "\"removeLabelIds\":[\"UNREAD\"]" +
	    "}";

	log.info("MarkEmailAsRead " + urlParameters);
	JSONObject jb = pubsubhttp.GetMyInstance().PerformPOST(targetURL, urlParameters);
	if (jb != null) {
	    log.info("Successfully Marked READ Message:" + messageID);
	    return true;	
	}
	log.info("Might not marked READ Message:" + messageID);
	return false;
    }

    public boolean MarkEmailAsProcessed(String messageID) {
	var processed = labels.get("Processed");
	var no_customer_found = labels.get("NoCustomerFound");
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/messages/" + messageID + "/modify";
	String urlParameters = "{" +
	    "\"addLabelIds\":[\"" + processed + "\"]," +
	    "\"removeLabelIds\":[\"" + no_customer_found + "\", \"UNREAD\", \"INBOX\"]" +
	    "}";

	log.info("MarkEmailAsProcessed " + urlParameters);
	JSONObject jb = pubsubhttp.GetMyInstance().PerformPOST(targetURL, urlParameters);
	if (jb != null) {
	    //log.info("MarkEmailAsProcessed" + messageID);
	    return true;	
	}
	
	log.info("MarkEmailAsProcessed: Might not marked READ Message:" + messageID);
	return false;
    }

    public boolean MarkEmailAsNotProcessed(String messageID) {
	var processed = labels.get("Processed");
	var no_customer_found = labels.get("NoCustomerFound");
	String targetURL = "https://www.googleapis.com/gmail/v1/users/me/messages/" + messageID + "/modify";
	String urlParameters = "{" +
	    "\"addLabelIds\":[\"" + no_customer_found + "\"]," +
	    "\"removeLabelIds\":[\"" + processed + "\", \"UNREAD\", \"INBOX\"]" +
	    "}";
	log.info("MarkEmailAsNotProcessed " + urlParameters);
	JSONObject jb = pubsubhttp.GetMyInstance().PerformPOST(targetURL, urlParameters);
	if (jb != null) {
	    // log.info("Successfully Marked READ Message:" + messageID);
	    return true;	
	}
	
	log.info("MarkEmailAsNotProcessed: Might not marked READ Message:" + messageID);
	return false;
    }
}
