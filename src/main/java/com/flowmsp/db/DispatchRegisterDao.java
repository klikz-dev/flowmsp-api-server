package com.flowmsp.db;

import com.flowmsp.domain.DispatchRegister;
import com.mongodb.client.MongoDatabase;

public class DispatchRegisterDao extends BaseDao<DispatchRegister> {
	public DispatchRegisterDao(MongoDatabase database) {
        super(database, DispatchRegister.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The DebugInfo collection is global so the slug is not prepended to the collection name
        return "DispatchRegister";
    }
}
