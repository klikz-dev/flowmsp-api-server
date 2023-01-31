package com.flowmsp.controller;

import spark.Request;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseListResponse<R> {
    public List<LinkRelation> links = null;
    public List<R>            data  = new ArrayList<>();
    public String			  msg = null;
    
    
    public void accept(R hydrantResponse) {
        data.add(hydrantResponse);
    }

    public void combine(BaseListResponse other) {
        data.addAll(other.data);
    }
    
    public void setServerMessage(String str) {
    	msg = str;
    }

    protected abstract void buildLinks(Request req);    
}
