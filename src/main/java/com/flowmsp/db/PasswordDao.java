package com.flowmsp.db;

import com.flowmsp.cache.RedisCache;
import com.flowmsp.domain.auth.Password;
import com.mongodb.client.MongoDatabase;

import java.util.Optional;

public class PasswordDao extends BaseDao<Password> {
    private RedisCache redisCache;

    public PasswordDao(MongoDatabase database, RedisCache redisCache) {
        super(database, Password.class, false);
        this.redisCache = redisCache;
    }

    @Override
    protected String getCollectionName(String slug) {
        // The password collection is global so the slug is not prepended to the collection name
        return "Password";
    }

    public Optional<Password> getByUsername(String username) {
        String key = makeRedisKey(username);

        Password p = redisCache.get(key, Password.class);
        if(p == null) {
            Optional<Password> op = getByFieldValue("username", username);
            op.ifPresent(password -> redisCache.set(key, password));
            return op;
        }
        else {
            return Optional.of(p);
        }
    }

    private String makeRedisKey(String username) {
        return String.format("password:%s", username);
    }
}
