package com.flowmsp.controller.dispatch;

import com.flowmsp.controller.messages.MsgSender;
import spark.Request;

import java.util.ArrayList;

public class DispatchResponse {
    public String errorMessage;
    public ArrayList<MsgSender> msgSenderList;
    public Long totalAmount;

    public static DispatchResponse build(String errorMessage) {
        DispatchResponse sr = new DispatchResponse();
        sr.errorMessage = errorMessage;
        return sr;
    }

    public static DispatchResponse build(Request req, ArrayList<MsgSender> msgSenderList) {
        DispatchResponse sr = new DispatchResponse();
        sr.msgSenderList = msgSenderList;
        return sr;
    }

    public static DispatchResponse build(Request req, ArrayList<MsgSender> msgSenderList, long totalAmount) {
        DispatchResponse sr = new DispatchResponse();
        sr.msgSenderList = msgSenderList;
        sr.totalAmount = totalAmount;
        return sr;
    }

}