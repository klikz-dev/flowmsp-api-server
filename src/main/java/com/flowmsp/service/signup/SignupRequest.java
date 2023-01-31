package com.flowmsp.service.signup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flowmsp.domain.Address;

/**
 * The SignupRequest is posted to the API server and passed to the SignupService methods when a new customer
 * creates their account.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SignupRequest {
    public String  customerName;
    public Address address;
    public String  email;
    public String  password;
    public String  firstName;
    public String  lastName;

    @Override
    public String toString() {
        return "SignupRequest{" +
                "customerName='" + customerName + '\'' +
                ", address=" + address +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
