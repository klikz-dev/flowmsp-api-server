package com.flowmsp.service.user;

public class UserRequest {
    public String email;
    public String firstName;
    public String lastName;
    public String password;
    public String role;

    @Override
    public String toString() {
        return "UserRequest{" +
                "email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
