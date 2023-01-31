package com.flowmsp.service;

import org.bson.BsonDouble;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.HashMap;
import java.util.Map;

public interface BsonUtil {
    static BsonValue convertToBsonValue(Object object) {
        if(object instanceof String) {
            return new BsonString((String)object);
        }
        else if (object instanceof Double) {
            return new BsonDouble((Double)object);
        }
        else if (object instanceof Integer) {
            return new BsonInt64((Integer)object);
        }
        else if (object instanceof Long) {
            return new BsonInt64((Long)object);
        }
        else {
            return null;
        }
    }

    static Object convertToObject(BsonValue bv) {
        if(bv instanceof BsonString) {
            return ((BsonString)bv).getValue();
        }
        else if (bv instanceof BsonDouble) {
            return ((BsonDouble) bv).getValue();
        }
        else if (bv instanceof BsonInt64) {
            return ((BsonInt64) bv).longValue();
        }
        else {
            return null;
        }
    }

    static Map<String, BsonValue> convertToBsonValueMap(Map<String, Object> map) {
        Map<String, BsonValue> bvMap = new HashMap<>();
        map.forEach((key,value) -> bvMap.put(key, convertToBsonValue(value)));
        return bvMap;
    }

    static Map<String, Object> convertToObjectMap(Map<String, BsonValue> map) {
        Map<String, Object> oMap = new HashMap<>();
        map.forEach((key,value) -> oMap.put(key, convertToObject(value)));
        return oMap;
    }
}
