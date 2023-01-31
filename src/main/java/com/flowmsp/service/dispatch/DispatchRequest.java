package com.flowmsp.service.dispatch;

public class DispatchRequest {
    public int limit;
    public String offsetId;

    public DispatchRequest() {
    }

    public DispatchRequest(int limit) {
        this.limit = limit;
    }

    public DispatchRequest(String offsetId) {
        this.offsetId = offsetId;
    }

    public DispatchRequest(int limit, String offsetId) {
        this.limit = limit;
        this.offsetId = offsetId;
    }
}