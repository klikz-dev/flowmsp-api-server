package com.flowmsp.domain.psap;

import org.bson.codecs.pojo.annotations.BsonId;

public class PSAP {
    @BsonId
    public String                 id;
    public String                 registryId;
    public String                 name;
    public String                 state;
    public String                 county;
    public String                 city;
    public String                 typeOfChange;
    public String                 comment;
    public String                 emailGateway;
    public String                 emailFormat;
}
