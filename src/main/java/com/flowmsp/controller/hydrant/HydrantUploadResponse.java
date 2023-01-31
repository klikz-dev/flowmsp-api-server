package com.flowmsp.controller.hydrant;

public class HydrantUploadResponse {
	public int successFlag;
	public String msg;
	public int recordKountUpdate;
	public int recordKountInsert;
	public int recordKountFail;
	public int recordKount;
	public String log;

    public HydrantUploadResponse() {
    	this.successFlag = 99;
        this.msg = "ERROR NOT RETRIEVED";
        this.recordKountUpdate = 0;
        this.recordKountInsert = 0;
        this.recordKountFail = 0;
        this.recordKount = 0;
        this.log = "";
    }

    public HydrantUploadResponse(int successFlag, String msg, int recordKountUpdate, int recordKountInsert, int recordKountFail, int recordKount, String log) {
        this.successFlag = successFlag;
        this.msg = msg;
        this.recordKountUpdate = recordKountUpdate;
        this.recordKountInsert = recordKountInsert;
        this.recordKountFail = recordKountFail;
        this.recordKount = recordKount;
        this.log = log;
    }
}
