package com.flowmsp.service;

import com.flowmsp.codec.ZonedDateTimeCodec;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public interface MongoUtil {
    static MongoDatabase initializeMongo(String uriString, String databaseName) {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).register("com.flowmsp.domain").build();

        CodecRegistry pojoCodecRegistry = fromRegistries(fromCodecs(new ZonedDateTimeCodec()),
                                                         MongoClient.getDefaultCodecRegistry(),
                                                         fromProviders(pojoCodecProvider));

        MongoClientURI uri           = new MongoClientURI(uriString, MongoClientOptions.builder().codecRegistry(pojoCodecRegistry));
        MongoClient    mongoClient   = new MongoClient(uri);
        MongoDatabase  mongoDatabase = mongoClient.getDatabase(databaseName);

        return mongoDatabase;
    }
}
