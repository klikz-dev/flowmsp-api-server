package com.flowmsp.domain.user;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.CustomerRef;
import com.flowmsp.domain.fcmData.FcmData;
import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import org.bson.BsonValue;

import java.util.*;

public class User {
    public String      href;
    public String      id;
    public String      email;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String      firstName;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String      lastName;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public UserRole    role;
    public CustomerRef customerRef;
    public Map<String, BsonValue> uiConfig = new HashMap<>();
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Boolean isOnDuty;


    public User() {
    }

    public User(String id, String email, String firstName, String lastName, UserRole role, Customer customer) {
        this.id          = id;
        this.email       = email;
        this.firstName   = firstName;
        this.lastName    = lastName;
        this.role        = role;
        this.customerRef = new CustomerRef(customer.id, customer.slug, customer.name);
        this.isOnDuty = true;
    }
}
