package com.flowmsp.domain.hydrant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.flow.FlowRange;
import com.flowmsp.domain.flow.PinLegend;
import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import java.util.Date;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class Hydrant {
    public String    id;
    public String    customerId;
    public String    customerSlug;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String    externalRef;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    @BsonProperty("lonLat")     // The database uses lonLat due to legacy issues, may want to adjust at some point
    public Point     latLon;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String    streetAddress;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Integer   flow;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Integer   size;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Boolean   inService;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Date		 outServiceDate;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Boolean   dryHydrant;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String    notes;
    public String    hydrantId;
    public FlowRange flowRange;
	public Date createdOn;
	public Date modifiedOn;
	public String createdBy;
	public String modifiedBy;
	public long batchNo;
	
    public Hydrant() {

    }

    public Hydrant(String id, Customer customer, String externalRef, Point latLon, Integer flow, Integer size, String streetAddress, Boolean inService, Date outServiceDate, Boolean dryHydrant, String notes) {
        this.id = id;
        this.customerId = customer.id;
        this.customerSlug = customer.slug;
        this.externalRef = externalRef;
        this.latLon = latLon;
        this.streetAddress = streetAddress;
        this.flow = flow;
        this.size = size;
        this.inService = inService;
        this.outServiceDate = outServiceDate;
        this.dryHydrant = dryHydrant;
        this.notes = notes;

        determineFlowRange(customer.pinLegend);
    }

    public Hydrant(String id, Customer customer, String externalRef, Double lat, Double lon, Integer flow, Integer size, String streetAddress, Boolean inService, Date outServiceDate, Boolean dryHydrant, String notes) {
        this(id, customer, externalRef, new Point(new Position(lon, lat)), flow, size, streetAddress, inService, outServiceDate, dryHydrant, notes);
    }

    @JsonIgnore
    public void determineFlowRange(PinLegend pinLegend) {
        this.flowRange = new FlowRange(pinLegend.unknownPinColor.label, pinLegend.unknownPinColor.pinColor);
        if(flow != null) {
            for(PinLegend.FlowRange fr : pinLegend.rangePinColors ) {
                if(flow >= fr.low && flow < fr.high) {
                    this.flowRange = new FlowRange(fr.label, fr.pinColor);
                }
            }
        }

    }
}
