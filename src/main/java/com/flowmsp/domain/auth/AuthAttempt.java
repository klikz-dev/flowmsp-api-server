package com.flowmsp.domain.auth;

import java.time.ZonedDateTime;

public class AuthAttempt {
    public ZonedDateTime  timestamp;
    public String         username;
    public String         remoteAddr;
    public AuthResultCode resultCode;
    public String         companySlug;
}
