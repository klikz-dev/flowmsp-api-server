package com.flowmsp.db;

import com.flowmsp.domain.debugpanel.debugInfo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

public class DebugInfoDao extends BaseDao<debugInfo> {
    
	public DebugInfoDao(MongoDatabase database) {
        super(database, debugInfo.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The DebugInfo collection is global so the slug is not prepended to the collection name
        return "DebugInfo";
    }    
}
