package com.flowmsp.service.partners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.CustomerDao;

public class partnersService {
	private static final Logger log = LoggerFactory.getLogger(partnersService.class);

	private final CustomerDao customerDao;
	private final ObjectMapper objectMapper;
	private final String mapAPIKey;
	public partnersService(CustomerDao customerDao, ObjectMapper objectMapper, String mapAPIKey) {
		this.customerDao = customerDao;
		this.objectMapper = objectMapper;
		this.mapAPIKey = mapAPIKey;
	}
}
