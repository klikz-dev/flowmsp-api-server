package com.flowmsp.db;

import com.flowmsp.domain.fcmData.FcmData;
import com.mongodb.client.MongoDatabase;

public class FcmDataDao extends BaseDao<FcmData> {
    public FcmDataDao(MongoDatabase database) {
        super(database, FcmData.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        return "FcmData";
    }
}
