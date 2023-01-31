package com.flowmsp.domain;

import java.util.Date;

import org.bson.codecs.pojo.annotations.BsonId;

public class DispatchRegister {
	@BsonId
    public String	id;
    public Date 	timeStamp;
    public Date 	expiryTimeStamp;
    public String	jwt;
    public String	customerId;
    public String	partnerId;
}
