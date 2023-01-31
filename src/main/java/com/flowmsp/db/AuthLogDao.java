package com.flowmsp.db;

import com.flowmsp.domain.auth.AuthLog;
import com.mongodb.client.MongoDatabase;

public class AuthLogDao extends BaseDao<AuthLog> {
    public AuthLogDao(MongoDatabase database) {super(database, AuthLog.class, false); }

    @Override
    protected String getCollectionName(String slug) {
        return "AuthLog";
    }
}
