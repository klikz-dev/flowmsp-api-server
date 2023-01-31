package com.flowmsp.controller.dispatch;

import spark.Request;

public class DispatchBadgeResponse {
    public String errorMessage;
    public long dispatchBadge;

    public static DispatchBadgeResponse build(String errorMessage) {
        DispatchBadgeResponse sr = new DispatchBadgeResponse();

        sr.errorMessage = errorMessage;

        return sr;
    }

    public static DispatchBadgeResponse build(Request req, long dispatchBadge) {
        DispatchBadgeResponse sr = new DispatchBadgeResponse();

        sr.dispatchBadge = dispatchBadge;

        return sr;
    }

}