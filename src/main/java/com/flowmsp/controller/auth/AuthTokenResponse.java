package com.flowmsp.controller.auth;

import com.flowmsp.controller.LinkRelation;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.pac4j.core.context.HttpConstants;
import spark.Request;

import java.util.List;

public class AuthTokenResponse {
    public List<LinkRelation> links;
    public String             accessToken;
    public String             tokenType;
    public Integer            expiresIn;

    public AuthTokenResponse() {

    }

    public AuthTokenResponse(String token) {
        this.accessToken = token;
        this.tokenType   = "Bearer";
        this.expiresIn   = 0;
    }

    public void buildLinks(Request req, String customerId, String slug, String userId, String errorMessage) {
        links = Lists.newArrayList();

        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        links.add(new LinkRelation("customer", HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/customer/%s", protocolHost, slug, customerId) ));
        links.add(new LinkRelation("user",     HttpConstants.HTTP_METHOD.GET, String.format("%s/api/%s/user/%s", protocolHost, slug, userId) ));
        links.add(new LinkRelation("customerID",     HttpConstants.HTTP_METHOD.GET, String.format("%s", customerId) ));
        if (!Strings.isNullOrEmpty(errorMessage)) {
        	links.add(new LinkRelation("msg",     HttpConstants.HTTP_METHOD.GET, String.format("%s", errorMessage) ));
        }
    }
}
