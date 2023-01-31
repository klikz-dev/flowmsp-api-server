package com.flowmsp.db;

import com.flowmsp.domain.fcmData.FcmData;
import com.flowmsp.domain.user.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class UserDao extends BaseDao<User> {
    public UserDao(MongoDatabase database) {
        super(database, User.class);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The user collection is customer specific so the slug is prepended to the collection name
        return slug.concat(".User");
    }

    @Override
    protected void createIndexes(MongoCollection collection) {
        super.createIndexes(collection);
        collection.createIndex(Indexes.ascending("username"));
    }

    public Optional<User> addUIConfig(String id, Map<String, BsonValue> config) {
        List<Bson> updates = new ArrayList<>();
        config.forEach((key, value) -> updates.add(set("uiConfig.".concat(key), value)));
        updateById(id, combine(updates));
        return getById(id);
    }

    public List<FcmData> getDutyFcmTokens() {
        ArrayList<FcmData> registrationTokens = new ArrayList<>();
        List<Document> documents = aggregate(
                Filters.or(Filters.eq("isOnDuty", true), Filters.eq("isOnDuty", null)),
                "FcmData", "_id", "userId", "fcmData"
        );

        for (Document document : documents) {
            List<Document> fcmDataList = (List<Document>) document.get("fcmData");
            for (Document fcmDataDocument : fcmDataList) {
                FcmData fcmData = new FcmData();
                fcmData.id = (String) fcmDataDocument.get("_id");
                fcmData.customerId = (String) fcmDataDocument.get("customerId");
                fcmData.userId = (String) fcmDataDocument.get("userId");
                fcmData.registrationToken = (String) fcmDataDocument.get("registrationToken");
                fcmData.platform = (String) fcmDataDocument.get("platform");
                fcmData.psapUnitCustomerIds = (List<String>) fcmDataDocument.get("psapUnitCustomerIds");
                registrationTokens.add(fcmData);
            }
        }

        return registrationTokens;
    }

}
