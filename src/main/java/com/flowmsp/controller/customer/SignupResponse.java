package com.flowmsp.controller.customer;

import com.flowmsp.controller.LinkRelation;
import com.flowmsp.controller.user.UserResponseBuilder;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.user.User;
import com.google.common.collect.Lists;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;

public class SignupResponse {
    public List<LinkRelation> links = null;

    public String errorMessage;

    public String customerId;
    public String customerSlug;
    public String customerName;
    public String userId;
    public String email;
    public String firstName;
    public String lastName;

    public static SignupResponse build(String errorMessage) {
        SignupResponse sr = new SignupResponse();

        sr.errorMessage = errorMessage;

        return sr;
    }

    public static SignupResponse build(Request req, Customer customer, User user) {
        SignupResponse sr = new SignupResponse();

        sr.customerId   = customer.id;
        sr.customerSlug = customer.slug;
        sr.customerName = customer.name;
        sr.userId       = user.id;
        sr.email        = user.email;
        sr.firstName    = user.firstName;
        sr.lastName     = user.lastName;

        sr.buildLinks(req);
        return sr;
    }

    private void buildLinks(Request req) {
        links = Lists.newArrayList();

        // Customer link
        links.add(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, CustomerResponse.selfLink(req, customerSlug, customerId)));

        // User link
        links.add(new LinkRelation("user", HttpConstants.HTTP_METHOD.GET, UserResponseBuilder.href(req, customerSlug, userId)));
    }

}
