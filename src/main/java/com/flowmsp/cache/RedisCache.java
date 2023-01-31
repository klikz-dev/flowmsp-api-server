package com.flowmsp.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCache {
    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private final StatefulRedisConnection<String, String> connection;
    private final ObjectMapper objectMapper;
    /**
     * When configured without a redis server URL the cache is disabled. This allows the clients of the cache
     * to interact with it identically whether it is enabled or not. All operations getting cached values
     * will return null when the cache is disabled.
     */
    public RedisCache() {
        log.info("Redis cache disabled");
        this.connection   = null;
        this.objectMapper = null;
    }

    /**
     * Initialize a connection to the redis server at the given URI.
     */
    public RedisCache(RedisClient redisClient, ObjectMapper objectMapper) {
        log.info("Redis cache enabled");
        this.connection = redisClient.connect();
        this.objectMapper = objectMapper;
    }

    public void shutdown() {
        log.info("Shutting down cache");
        if(connection != null) { connection.close(); }
    }

    public <T> void set(String key, T obj) {
        if(connection == null) return;

        try {
            String json = objectMapper.writeValueAsString(obj);
            connection.sync().set(key, json);
        }
        catch(Exception e) {
            log.error("Failed to write value to redis cache", e);
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        if(connection == null) return null;

        try {
            String json = connection.sync().get(key);
            if(json != null) {
                return objectMapper.readValue(json, clazz);
            }
            else {
                return null;
            }
        }
        catch(Exception e) {
            log.error("Failed retrieving value from redis cache", e);
            return null;
        }
    }
}
