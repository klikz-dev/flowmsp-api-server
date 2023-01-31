package com.flowmsp.domain.fcmData;

import org.bson.codecs.pojo.annotations.BsonId;

import java.util.Date;
import java.util.List;

public class FcmData {

    @BsonId
    public String id;
    public String customerId;
    public String userId;
    public String registrationToken;
    public String platform;
    public Date createdOn;
    public Date modifiedOn;
    public List<String> psapUnitCustomerIds;

    public FcmData() {
    }

    public FcmData(String id, String customerId, String userId, String registrationToken, String platform) {
        this.id = id;
        this.customerId = customerId;
        this.userId = userId;
        this.registrationToken = registrationToken;
        this.platform = platform;
    }
}
