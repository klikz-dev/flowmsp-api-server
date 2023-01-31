package com.flowmsp.domain.auth;

public class RegistrationLink {
    public String id;
    public String username;
    public String userId;
    public String customerId;
    public String customerSlug;
    public String linkPart;
    public long time;
    public boolean used;

    public RegistrationLink() {

    }

    public RegistrationLink(String id, String username, String userId, String customerId, String customerSlug, String linkPart, long time, boolean used) {
        this.id = id;
        this.username = username;
        this.userId = userId;
        this.customerId = customerId;
        this.customerSlug = customerSlug;
        this.linkPart = linkPart;
        this.time = time;
        this.used = used;
    }
}
