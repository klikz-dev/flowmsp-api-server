package com.flowmsp.db;

import com.flowmsp.domain.Message;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;

public class MessageDao extends BaseDao<Message> {
    public MessageDao(MongoDatabase database) { super(database, Message.class, true); }

    @Override
    protected String getCollectionName(String slug) {
        return slug.concat(".MsgReceiver");
    }

    @Override
    protected void createIndexes(MongoCollection collection) {
        super.createIndexes(collection);
        collection.createIndex(Indexes.ascending("messageID"));
    }
}
