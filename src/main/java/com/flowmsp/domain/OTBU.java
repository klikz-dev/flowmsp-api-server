package com.flowmsp.domain;

import java.util.Date;

import org.bson.codecs.pojo.annotations.BsonId;

public class OTBU {
	@BsonId
    public String	id;
    public int		version;
    public String	description;
    public Date 	timeStamp;
}
