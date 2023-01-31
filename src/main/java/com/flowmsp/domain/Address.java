package com.flowmsp.domain;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

/**
 * Common class for address information, intended to be embedded in other objects which
 * have address information.
 */
public class Address {
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String address1;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String address2;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String city;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String state;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String zip;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Point  latLon;

    public Address() {

    }

    public Address(String address1, String address2, String city, String state, String zip, Point latLon) {
        this.address1 = address1;
        this.address2 = address2;
        this.city     = city;
        this.state    = state;
        this.zip      = zip;
        this.latLon   = latLon;
    }

    public Address(String address1, String address2, String city, String state, String zip, Double lat, Double lon) {
        this(address1, address2, city, state, zip, new Point(new Position(lat, lon)));
    }
}
