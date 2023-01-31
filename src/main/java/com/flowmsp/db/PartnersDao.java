package com.flowmsp.db;

import com.flowmsp.domain.partners.Partners;
import com.mongodb.client.MongoDatabase;

public class PartnersDao extends BaseDao<Partners> {
	public PartnersDao(MongoDatabase database) { super(database, Partners.class, false); }

    @Override
    protected String getCollectionName(String slug) {
        return "Partners";
    }
}
