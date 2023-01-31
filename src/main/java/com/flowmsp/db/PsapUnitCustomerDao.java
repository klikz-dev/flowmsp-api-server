package com.flowmsp.db;

import com.flowmsp.domain.psap.PsapUnitCustomer;
import com.mongodb.client.MongoDatabase;

public class PsapUnitCustomerDao extends BaseDao<PsapUnitCustomer> {
    public PsapUnitCustomerDao(MongoDatabase database) {
        super(database, PsapUnitCustomer.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        return "PsapUnitCustomer";
    }
}
