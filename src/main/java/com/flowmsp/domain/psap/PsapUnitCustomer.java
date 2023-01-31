package com.flowmsp.domain.psap;

import org.bson.codecs.pojo.annotations.BsonId;

public class PsapUnitCustomer {
    @BsonId
    public String id;
    public String slug;
    public String customerId;
    public String psapId;
    public String unit;
    public String unit_type;
    public String vehicle_no;
    public String station;
    public String dept;
}
