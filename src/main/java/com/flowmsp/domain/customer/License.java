package com.flowmsp.domain.customer;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;

import java.util.Date;

public class License {
    @AllowedPatches({PatchOp.REPLACE})
    public LicenseType licenseType;
    @AllowedPatches({PatchOp.REPLACE})
    public LicenseTerm licenseTerm;
    public Date creationTimestamp;
    @AllowedPatches({PatchOp.REPLACE})
    public Date expirationTimestamp;

    public License() {

    }

    public License(LicenseType type, LicenseTerm term) {
        this.licenseType = type;
        this.licenseTerm = term;
    }
}
