package com.flowmsp.controller;

import org.pac4j.core.context.HttpConstants;

public class LinkRelation {
    public String rel;
    public String op;
    public String href;

    public LinkRelation(String rel, HttpConstants.HTTP_METHOD op, String href) {
        this.rel  = rel;
        this.op   = op.toString();
        this.href = href;
    }
}