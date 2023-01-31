package com.flowmsp.controller.psap;

import spark.Request;

import java.util.List;

public class PsapUnitCustomerResponse {

    public String errorMessage;
    public List<PsapUnitCustomerModel> unitsList;

    public static PsapUnitCustomerResponse build(String errorMessage) {
        PsapUnitCustomerResponse sr = new PsapUnitCustomerResponse();

        sr.errorMessage = errorMessage;

        return sr;
    }

    public static PsapUnitCustomerResponse build(Request req, List<PsapUnitCustomerModel> unitsList) {
        PsapUnitCustomerResponse sr = new PsapUnitCustomerResponse();

        sr.unitsList = unitsList;

        return sr;
    }
}
