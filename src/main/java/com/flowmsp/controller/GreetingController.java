package com.flowmsp.controller;

import org.pac4j.core.context.HttpConstants;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GreetingController {
    public Map<String, Object> getGreeting(Request req, Response res) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());

        Map<String, Object> maps = new HashMap<>();

        maps.put("greeting", "Welcome to the FlowMSP API. For more information about FlowMSP visit http://flowmsp.com");

        List<LinkRelation> links = new ArrayList<>();
        links.add(new LinkRelation("signup", HttpConstants.HTTP_METHOD.POST, String.format("%s/api/signup", protocolHost)));
        links.add(new LinkRelation("auth",   HttpConstants.HTTP_METHOD.POST, String.format("%s/api/auth/token", protocolHost)));
        links.add(new LinkRelation("doc",    HttpConstants.HTTP_METHOD.POST, String.format("https://documenter.getpostman.com/view/2293732/flowmsp-api/6fVVQNQ")));

        maps.put("links", links);
        return maps;
    }
}
