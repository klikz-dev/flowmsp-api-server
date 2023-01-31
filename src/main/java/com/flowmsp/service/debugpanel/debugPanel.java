package com.flowmsp.service.debugpanel;

import java.util.Date;
import java.util.Optional;

import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.debugpanel.debugInfo;
import com.flowmsp.service.profile.InvalidProfileException;
import com.flowmsp.service.profile.MissingProfileException;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.google.common.base.Strings;

import spark.Request;
import spark.Response;

public class debugPanel {
	private final DebugInfoDao debugInfoDao;
	private debugInfo info;

	public debugPanel(DebugInfoDao debugInfoDao) {
		this.debugInfoDao = debugInfoDao;
		info = new debugInfo();
	}
		
	public void SetData(String parameter, Object value) {
		if (parameter.equalsIgnoreCase("source")) {
			info.Source = value.toString();
		} else if (parameter.equalsIgnoreCase("ErrorFlag")) {
			info.ErrorFlag =  (int)value;
		} else if (parameter.equalsIgnoreCase("ErrorDescription")) {
			info.ErrorDescription =  value.toString();
		} else {
			if (value != null) {
				info.Details.put(parameter, value.toString());	
			}				
		}
	}
	
	public void commit() {
		//Do we need to save repetitive information. 
		// I don't think so
		String MessageID = info.Details.get("MessageID");
		boolean alreadyAdded = false;
		if (!Strings.isNullOrEmpty(MessageID)) {
			Optional<debugInfo> chk = debugInfoDao.getByFieldValue("Details.MessageID", MessageID);		
			if (chk.isPresent()) {
				alreadyAdded = true;
			}
		}	
		if (!alreadyAdded) {
			info.TimeStamp = new Date();
			debugInfoDao.save(info);			
		}
	}
	
	public void SetCustomerInfo(Request req, Response res, String subject) {
        ValidatedProfile profile;
		try {
			profile = ProfileUtil.getValidatedProfile(req, res);
	        String customerId = profile.getCustomerId();
	        String customerSlug = profile.getCustomerSlug();
	        String userId = profile.getUserId();
	        String userName = profile.getUserName();
	        
	        SetData("customerId", customerId);
	        SetData("customerSlug", customerSlug);
	        SetData("userId", userId);
	        SetData("userName", userName);
	        
	        //Now get version & source
	        String source = req.headers("X-FlowMSP-Source");
	        String version = req.headers("X-FlowMSP-Version");
	        if (source == null) {
	        	source = "UNKNOWN";
	        }
	        if (version == null) {
	        	version = "UNKNOWN";
	        }
	        
	        SetData("source", source);
	        SetData("version", version);
	        SetData("subject", subject);
		} catch (MissingProfileException e) {
			e.printStackTrace();
		} catch (InvalidProfileException e) {
			e.printStackTrace();
		}
	}
	
	public void commitLog() {
		//Here we need not check duplicacy, just add
		info.TimeStamp = new Date();
		debugInfoDao.save(info);
	}
}
