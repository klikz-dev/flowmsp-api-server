package com.flowmsp.db;

import com.flowmsp.domain.auth.RegistrationLink;
import com.mongodb.client.MongoDatabase;

import java.util.Optional;


public class RegistrationLinkDao extends BaseDao<RegistrationLink> {

    public RegistrationLinkDao(MongoDatabase database) {
        super(database, RegistrationLink.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        return "RegistrationLink";
    }

    public Optional<RegistrationLink> getByLinkPart(String linkPart) {
        return getByFieldValue("linkPart", linkPart);
    }

    public Optional<RegistrationLink> getByUsername(String username) {
        return getByFieldValue("username", username);
    }

}
