package com.flowmsp.db;

import com.flowmsp.domain.psap.PSAP;
import com.mongodb.client.MongoDatabase;

public class PSAPDao extends BaseDao<PSAP> {
    public PSAPDao(MongoDatabase database) {
        super(database, PSAP.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        return "PSAP";
    }
}
