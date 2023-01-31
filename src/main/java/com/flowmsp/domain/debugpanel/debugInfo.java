package com.flowmsp.domain.debugpanel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class debugInfo {
	public String id;
	public String Source;
	public Date TimeStamp;
    public int	ErrorFlag;
    public String ErrorDescription;
    public Map<String, String> Details;
	public debugInfo() {
		id = UUID.randomUUID().toString();
		Source = "";
		ErrorFlag = 0;
		ErrorDescription = "";
		Details = new HashMap<String, String>();
	}
	
	public debugInfo(String Source, Date TimeStamp, int ErrorFlag, String ErrorDescription, Map<String, String> Details) {
		this.Source = Source;
		this.TimeStamp = TimeStamp;
		this.ErrorFlag = ErrorFlag;
		this.ErrorDescription = ErrorDescription;
		this.Details = Details;
	}
}
