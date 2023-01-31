package com.flowmsp.service.user;

public class UserMainDataRequest {
    public String email;
    public String role;

    @Override
    public String toString() {
        return "UserRequest{" +
                "email='" + email + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
