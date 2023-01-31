package com.flowmsp.domain.location;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;

/**
 * Building Data will be stored here
 */
public class Building {
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String occupancyType;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String constructionType;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String roofType;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String roofConstruction;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String roofMaterial;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String sprinklered;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String standPipe;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String fireAlarm;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String normalPopulation;    
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String hoursOfOperation;    
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String ownerContact;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String ownerPhone;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String originalPrePlan;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String lastReviewedOn;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String lastReviewedBy;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String notes;
    public Building() {

    }

    public Building(String occupancyType, String constructionType, String roofType, String roofConstruction, 
    		String roofMaterial, String sprinklered, String standPipe, String fireAlarm,
    		String normalPopulation, String hoursOfOperation, 
    		String ownerContact, String ownerPhone, String originalPrePlan,
    		String lastReviewedOn, String lastReviewedBy, String notes) {
        this.occupancyType		= occupancyType;
        this.constructionType	= constructionType;
        this.roofType     		= roofType;
        this.roofConstruction   = roofConstruction;
        this.roofMaterial      	= roofMaterial;
        this.sprinklered		= sprinklered;
        this.standPipe			= standPipe;
        this.fireAlarm			= fireAlarm;
        this.normalPopulation   = normalPopulation;
        this.hoursOfOperation   = hoursOfOperation;        
        this.ownerContact 		= ownerContact;
        this.ownerPhone 		= ownerPhone;
        this.originalPrePlan    = originalPrePlan;
        this.lastReviewedOn    	= lastReviewedOn;
        this.lastReviewedBy     = lastReviewedBy;
        this.notes				= notes;
    }
}