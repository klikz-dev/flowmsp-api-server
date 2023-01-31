package com.flowmsp.domain.location;

import com.flowmsp.domain.Address;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.service.FlowService;
import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import com.mongodb.client.model.geojson.Polygon;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A location is defined by both its address as well as the polygon outlining the phyiscal location.
 * A location is also associated with a set of hydrants. There is a one-to-one relationship between
 * hydrants and locations, this is modeled in Mongo with the entire hydrant entity being stored in the
 * hydrants table as well as being stored fully for each location the hydrant is associated with. This
 * simplifies the code required to handle location planning. Locations are not stored with the hydrants.
 */
public class Location {
    public String id;
    public String customerId;
    public String customerSlug;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String name;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public int storey;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public int storeyBelow;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String lotNumber;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Long roofArea;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Long requiredFlow;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Polygon geoOutline;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Address address;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public List<String> hydrants = new ArrayList<>();
    public List<Image>  images   = new ArrayList<>();
	@AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
	public Building building;
	public Date createdOn;
	public Date modifiedOn;
	public String createdBy;
	public String modifiedBy;
	public long batchNo;
	
    public Location() {}

    public Location(String id, Customer customer, String name, int storey, int storeyBelow, String lotNumber, Polygon geoOutline, Address address, List<String> hydrants, Building building){
        this.id = id;
        this.customerId = customer.id;
        this.customerSlug = customer.slug;
        this.name = name;
        this.storey = storey;
        this.storeyBelow = storeyBelow;
        this.lotNumber = lotNumber;
        this.roofArea     = FlowService.calcArea(geoOutline);
        this.requiredFlow = FlowService.calcRequiredFlow(roofArea);
        this.geoOutline = geoOutline;
        this.address = address;
        this.hydrants = hydrants;
        this.building = building;
    }
}
