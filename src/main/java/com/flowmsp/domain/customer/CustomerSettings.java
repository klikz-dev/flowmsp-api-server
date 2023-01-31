package com.flowmsp.domain.customer;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;

public class CustomerSettings {
    // All preplanning distances are in feet or square feet
    // All preplanning flow rates are in GPM (gallons/minute)
    @AllowedPatches({PatchOp.REPLACE})
    public int preplanningAreaRounding;                 // When computing building area, round to this amount
    @AllowedPatches({PatchOp.REPLACE})
    public int preplanningMaxHydrants;                  // Maximum number of hydrants to preplan with a building
    @AllowedPatches({PatchOp.REPLACE})
    public int preplanningMaxAreaForFlowComputation;    // Cap the are of a building to this amout when computing the required flow
    @AllowedPatches({PatchOp.REPLACE})
    public int preplanningMaxDistanceForHydrantSearch;  // Maximum distance a fire hydrant can be from building

    // Hydrant settings, all distances are in feet
    @AllowedPatches({PatchOp.REPLACE})
    public int minimumNewHydrantDistance;               // The minimum distance any hydrant must be for a new hydrant to be uploaded
}
