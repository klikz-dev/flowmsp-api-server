package com.flowmsp.domain.auth;

import org.bson.codecs.pojo.annotations.BsonId;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class  AuthLog {
    @BsonId
    public ZonedDateTime          timestampHour;
    public ArrayList<AuthAttempt> authAttempts;

    public AuthLog() { }

    public AuthLog(ZonedDateTime timestampHour) {
        this.timestampHour = timestampHour;
        this.authAttempts  = new ArrayList<>();
    }
}
