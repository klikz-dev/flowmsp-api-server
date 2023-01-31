package com.flowmsp.domain.customer;

import com.flowmsp.domain.Address;
import com.flowmsp.domain.flow.PinColor;
import com.flowmsp.domain.flow.PinLegend;
import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import com.mongodb.client.model.geojson.Polygon;
import org.bson.BsonValue;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.*;

public class Customer {
    @BsonId
    public String                 id;
    public String                 slug;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String                 name;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Address                address;
    @AllowedPatches({PatchOp.REPLACE})
    public License                license;
    public PinLegend              pinLegend  = defaultPinLegend();
    @AllowedPatches({PatchOp.REPLACE})
    public CustomerSettings       settings   = defaultCustomerSettings();
    public Map<String, BsonValue> uiConfig   = new HashMap<>();
    @AllowedPatches({PatchOp.REPLACE})
    public String smsNumber;
    @AllowedPatches({PatchOp.REPLACE})
    public String emailGateway;
    @AllowedPatches({PatchOp.REPLACE})
    public String smsFormat;
    @AllowedPatches({PatchOp.REPLACE})
    public String emailFormat;
    @AllowedPatches({PatchOp.REPLACE})
    public String emailSignature;
    @AllowedPatches({PatchOp.REPLACE})
    public String emailSignatureLocation;
    
    @AllowedPatches({PatchOp.REPLACE})
	public String fromContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String toContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String subjectContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String bodyContains;

	@AllowedPatches({PatchOp.REPLACE})
	public String fromNotContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String toNotContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String subjectNotContains;
	@AllowedPatches({PatchOp.REPLACE})
	public String bodyNotContains;
    
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public double			   boundSWLat;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public double			   boundSWLon;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public double			   boundNELat;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public double			   boundNELon;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public boolean			   dataSharingConsent;
    public boolean			   dispatchSharingConsent;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String			   timeZone;

    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Date demoDispatch;

    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Polygon districtOutline;

    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String SFTP_userid;

    public Customer() {

    }

    public Customer(String id, String slug, String name, Address address, License license, String smsNumber, String emailGateway, String smsFormat, String emailFormat, String emailSignature, String emailSignatureLocation,
    		String fromContains, String toContains, String subjectContains, String bodyContains, 
    		String fromNotContains, String toNotContains, String subjectNotContains, String bodyNotContains,
    		double boundSWLat, double boundSWLon, double boundNELat, double boundNELon, boolean dataSharingConsent, boolean dispatchSharingConsent, String timeZone) {
        this.id      = id;
        this.slug    = slug;
        this.name    = name;
        this.address = address;
        this.license = license;
        this.smsNumber = smsNumber;
        this.emailGateway = emailGateway;
        this.smsFormat = smsFormat;
        this.emailFormat = emailFormat;
        this.emailSignature = emailSignature;
        this.emailSignatureLocation = emailSignatureLocation;
        
        this.fromContains = fromContains;
        this.toContains = toContains;
        this.subjectContains = subjectContains;
        this.bodyContains = bodyContains;

        this.fromNotContains = fromNotContains;
        this.toNotContains = toNotContains;
        this.subjectNotContains = subjectNotContains;
        this.bodyNotContains = bodyNotContains;
        
        this.boundSWLat = boundSWLat;
        this.boundSWLon = boundSWLon;
        this.boundNELat = boundNELat;
        this.boundNELon = boundNELon;
        this.dataSharingConsent = dataSharingConsent;
        this.dispatchSharingConsent = dispatchSharingConsent;
        this.timeZone = timeZone;
    }
    
    public Customer(String id, String slug, String name, Address address, License license) {
    	this(id, slug, name, address, license, "", "", "", "", "", "", "", "", "", "", "", "", "", "", 0, 0, 0, 0, false, false, "");
    }
    
    private static PinLegend defaultPinLegend() {
        PinLegend newPinLegend = new PinLegend();
        newPinLegend.unknownPinColor = new PinLegend.FlowRange("Unknown", 0, 0, PinColor.YELLOW);
        newPinLegend.rangePinColors  = new ArrayList<>();
        newPinLegend.rangePinColors.add(new PinLegend.FlowRange("0 to less than 500 GPM",     0,   500,    PinColor.RED));
        newPinLegend.rangePinColors.add(new PinLegend.FlowRange("500 to less than 1000 GPM",  500, 1000,   PinColor.ORANGE));
        newPinLegend.rangePinColors.add(new PinLegend.FlowRange("1000 to less than 1500 GPM", 1000,1500,   PinColor.GREEN));
        newPinLegend.rangePinColors.add(new PinLegend.FlowRange("1500+ GPM",                  1500,100000, PinColor.BLUE));
        return newPinLegend;
    }

    private static CustomerSettings defaultCustomerSettings() {
        CustomerSettings customerSettings = new CustomerSettings();
        customerSettings.preplanningAreaRounding                = 100;
        customerSettings.preplanningMaxHydrants                 = 10;
        customerSettings.preplanningMaxAreaForFlowComputation   = 20000;
        customerSettings.preplanningMaxDistanceForHydrantSearch = 5000;
	customerSettings.minimumNewHydrantDistance              = 100;

        return customerSettings;
    }
}
