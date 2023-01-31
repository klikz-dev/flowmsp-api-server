package com.flowmsp.db;

import com.flowmsp.domain.dispatch.DispatchReadStatus;
import com.mongodb.client.MongoDatabase;

public class DispatchReadStatusDao extends BaseDao<DispatchReadStatus> {
    public DispatchReadStatusDao(MongoDatabase database) {
        super(database, DispatchReadStatus.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        return "DispatchReadStatus";
    }
}
