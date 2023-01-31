package com.flowmsp.db;

import com.flowmsp.domain.OTBU;
import com.mongodb.client.MongoDatabase;
//One Time Batch Updation (OTBU)

public class OTBUDao extends BaseDao<OTBU> {
	public OTBUDao(MongoDatabase database) {
        super(database, OTBU.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The DebugInfo collection is global so the slug is not prepended to the collection name
        return "OTBU";
    }    
}
