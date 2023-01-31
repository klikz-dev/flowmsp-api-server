package com.flowmsp.service.signup;

import com.flowmsp.domain.user.User;
import com.flowmsp.domain.customer.Customer;

/**
 * The SignupResult is created by the SignupService after a new account is created. It contains the newly created
 * Customer along with the initial User.
 */
public class SignupResult {
    public String   errorMessage;
    public Customer customer;
    public User     user;

    public SignupResult() {}

    public SignupResult(Customer c, User u) {
        this.customer = c;
        this.user     = u;
    }

    public SignupResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
