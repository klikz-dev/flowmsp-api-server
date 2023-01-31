package com.flowmsp.service.user;

public class ResendRegistrationLinkRequest {
    public String email;

    @Override
    public String toString() {
        return "UserRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
