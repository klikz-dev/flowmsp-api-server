package com.flowmsp.domain.user;

import org.bson.codecs.pojo.annotations.BsonId;

import java.time.ZonedDateTime;

public class PasswordResetRequest {
    @BsonId
    public String        id;
    public String        completionUrl;
    public String        email;
    public ZonedDateTime created;
    public ZonedDateTime attempted;
}
