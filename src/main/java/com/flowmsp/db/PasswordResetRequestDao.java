package com.flowmsp.db;

import com.flowmsp.domain.user.PasswordResetRequest;
import com.mongodb.client.MongoDatabase;

public class PasswordResetRequestDao extends BaseDao<PasswordResetRequest> {

    public PasswordResetRequestDao(MongoDatabase database) {
        super(database, PasswordResetRequest.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The customer collection is global so the slug is not prepended to the collection name
        return "PasswordResetRequest";
    }
}
