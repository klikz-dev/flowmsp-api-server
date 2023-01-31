package com.flowmsp.domain.customer;

public class CustomerRef {
    public String customerId;
    public String customerSlug;
    public String customerName;
    public String href;

    public CustomerRef() {

    }

    public CustomerRef(String customerId, String customerSlug, String customerName) {
        this.customerId   = customerId;
        this.customerSlug = customerSlug;
        this.customerName = customerName;
    }
}
