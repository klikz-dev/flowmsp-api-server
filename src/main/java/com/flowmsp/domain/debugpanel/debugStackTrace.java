package com.flowmsp.domain.debugpanel;

import java.util.HashMap;
import java.util.Map;

import org.bson.codecs.pojo.annotations.BsonId;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;

public class debugStackTrace {
	@BsonId
	public String 			   id;
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public String 			   timeStamp;
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public String			   name;
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public Map<String, Object> inParameters;
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public Map<String, Object> operation;
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public Map<String, Object> outParamaters;
	
	public debugStackTrace() {
		timeStamp = String.valueOf(System.currentTimeMillis()); //timestamp
		name = ""; //functionName
		inParameters = new HashMap<String, Object>();
		outParamaters = new HashMap<String, Object>();
		operation = new HashMap<String, Object>();
	}	
}
