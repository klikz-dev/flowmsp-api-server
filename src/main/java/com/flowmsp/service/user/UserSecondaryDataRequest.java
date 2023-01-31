package com.flowmsp.service.user;

public class UserSecondaryDataRequest {
    public String firstName;
    public String lastName;
    public String password;

    @Override
    public String toString() {
        return "UserRequest{" +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
