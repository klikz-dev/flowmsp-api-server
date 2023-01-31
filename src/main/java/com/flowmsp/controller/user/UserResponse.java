package com.flowmsp.controller.user;

import com.flowmsp.domain.user.UserRole;
import com.flowmsp.domain.customer.CustomerRef;

import java.util.HashMap;
import java.util.Map;

/**
 * The entity serialized through the API when returning users. Required because the database object has a map of
 * BsonValue for the uiConfig which is a mess to serialize. For the response we convert the uiConfig to a map of
 * Object.
 */
public class UserResponse {
    public String      href;
    public String      id;
    public String      email;
    public String      firstName;
    public String      lastName;
    public UserRole    role;
    public CustomerRef customerRef;
    public Map<String, Object> uiConfig = new HashMap<>();
    public Boolean     isOnDuty;
    public String      registrationLink;
}
