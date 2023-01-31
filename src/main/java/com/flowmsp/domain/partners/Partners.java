package com.flowmsp.domain.partners;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;

public class Partners {
	public String    id;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String    customerId;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String    partnerId;

    public Partners() {
    	
    }
    
    public Partners(String id, String customerId, String partnerId) {
        this.id = id;
        this.customerId = customerId;
        this.partnerId = partnerId;
    }
}
