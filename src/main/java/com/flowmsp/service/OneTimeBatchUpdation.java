package com.flowmsp.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.LocationDao;
import com.flowmsp.db.OTBUDao;
import com.flowmsp.domain.OTBU;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.partners.partnersService;
import com.flowmsp.service.signup.SignupService;
import com.google.common.base.Strings;

public class OneTimeBatchUpdation {
	private static final Logger log = LoggerFactory.getLogger(partnersService.class);

	private final CustomerDao customerDao;
	private final LocationDao locationDao;
	private final OTBUDao otbuDao;
	private final ObjectMapper objectMapper;
	private final int version;
	private final SignupService signUpService;
	
	public OneTimeBatchUpdation(SignupService signUpService, CustomerDao customerDao, LocationDao locationDao, OTBUDao otbuDao, ObjectMapper objectMapper, int version) {
		this.signUpService = signUpService;
		this.customerDao = customerDao;
		this.locationDao = locationDao;
		this.otbuDao = otbuDao;
		this.objectMapper = objectMapper;
		this.version = version;
	}
	
	public void StartUpdate() {
		if (otbuDao.getByFieldValue("version", version).isPresent()) {
			//Already Updated
			return;
		}
		//Here we should run the update & version should be incremented
		if (version <= 1) {
			// Insert version
			List<Customer> customers = customerDao.getAll();
			for (int ii = 0; ii < customers.size(); ii ++) {
				Customer c = customers.get(ii);
				signUpService.UpdateCustomerGeoLocation(c);
			}
			OTBU t1 = new OTBU();
			t1.id = "";
			t1.version = version;
			t1.description = "Update geoLocations";
			t1.timeStamp = new Date();
			otbuDao.save(t1);
		}
		if (version <= 2) {
			List<Customer> customers = customerDao.getAll();
			for (int ii = 0; ii < customers.size(); ii ++) {
				Customer c = customers.get(ii);
				if (Strings.isNullOrEmpty(c.emailGateway)) {
					continue;
				}
				if (Strings.isNullOrEmpty(c.emailSignature)) {
					continue;
				}
				if (Strings.isNullOrEmpty(c.emailSignatureLocation)) {
					continue;
				}

				if (c.emailSignatureLocation.equalsIgnoreCase("subject")) {
					if (!Strings.isNullOrEmpty(c.subjectContains)) {
						continue;
					}
		            Bson newValue = new Document("subjectContains", c.emailSignature);
		            Bson updateOperationDocument = new Document("$set", newValue);
		            customerDao.updateById(c.id, updateOperationDocument);      
				} else {
					if (!Strings.isNullOrEmpty(c.bodyContains)) {
						continue;
					}
		            Bson newValue = new Document("bodyContains", c.emailSignature);
		            Bson updateOperationDocument = new Document("$set", newValue);
		            customerDao.updateById(c.id, updateOperationDocument);      
				}
			}
			
			for (int ii = 0; ii < customers.size(); ii ++) {
				Customer c = customers.get(ii);
				SlugContext.setSlug(c.slug);
				try {
					List<Location> locationBusiness = locationDao.getAllByFieldValue("building.occupancyType", "Business");
					List<Location> locationMercantile = locationDao.getAllByFieldValue("building.occupancyType", "Mercantile");
					if (locationBusiness.isEmpty() && locationMercantile.isEmpty()) {
						SlugContext.clearSlug();
						continue;
					}
		            Bson newValue = new Document("building.occupancyType", "Business / Mercantile");
		            Bson updateOperationDocument = new Document("$set", newValue);

					locationDao.updateAllByFieldValue("building.occupancyType", "Business", updateOperationDocument);
					locationDao.updateAllByFieldValue("building.occupancyType", "Mercantile", updateOperationDocument);
					
				} catch (Exception ex) {
					log.error("Error in batch updation:" + ex);
				}
				SlugContext.clearSlug();
			}
			
			OTBU t1 = new OTBU();
			t1.id = UUID.randomUUID().toString();
			t1.version = version;
			t1.description = "Update dispatch";
			t1.timeStamp = new Date();
			otbuDao.save(t1);
		}
	}
}
