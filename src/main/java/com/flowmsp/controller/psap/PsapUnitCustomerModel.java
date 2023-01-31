package com.flowmsp.controller.psap;

import com.flowmsp.domain.psap.PsapUnitCustomer;

public class PsapUnitCustomerModel {
    public String id;
    public String slug;
    public String customerId;
    public String psapId;
    public String unit;
    public String unit_type;
    public String vehicle_no;
    public String station;
    public String dept;

    public Boolean selected;

    public static PsapUnitCustomerModel map(PsapUnitCustomer psapUnitCustomer) {
        PsapUnitCustomerModel psapUnitCustomerModel = new PsapUnitCustomerModel();

        psapUnitCustomerModel.id = psapUnitCustomer.id;
        psapUnitCustomerModel.slug = psapUnitCustomer.slug;
        psapUnitCustomerModel.customerId = psapUnitCustomer.customerId;
        psapUnitCustomerModel.psapId = psapUnitCustomer.psapId;
        psapUnitCustomerModel.unit = psapUnitCustomer.unit;
        psapUnitCustomerModel.unit_type = psapUnitCustomer.unit_type;
        psapUnitCustomerModel.vehicle_no = psapUnitCustomer.vehicle_no;
        psapUnitCustomerModel.station = psapUnitCustomer.station;
        psapUnitCustomerModel.dept = psapUnitCustomer.dept;

        psapUnitCustomerModel.selected = false;

        return psapUnitCustomerModel;
    }
}
