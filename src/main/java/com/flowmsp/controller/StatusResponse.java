package com.flowmsp.controller;

public class StatusResponse {
    public boolean			  success = false;
    public String			  message = null;

    public StatusResponse() {
    }

    public StatusResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
