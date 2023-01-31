package com.flowmsp.service.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonPatch;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchUtil {
    private static final Logger log = LoggerFactory.getLogger(PatchUtil.class);
    private static ObjectMapper objectMapper = null;
    private static Map<Class<?>, Map<String, EnumMap<PatchOp, Boolean>>> cache = new HashMap<>();

    public static void intitialize(ObjectMapper om) {
        objectMapper = om;
    }

    public static <T> T patch(String patchDocument, T origObject, Class<T> clazz) throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        if(objectMapper != null)
        {
            JsonNode patchJson = objectMapper.readTree(patchDocument);
            JsonNode customerJson = objectMapper.valueToTree(origObject);

            if(!patchJson.isArray())
            {
                throw new RuntimeException("Patch is invalid format");
            }
            for(JsonNode singleOp : patchJson)
            {
                PatchOp op = PatchOp.valueOf(singleOp.get("op").asText().toUpperCase());
                String jsonPointer = singleOp.get("path").asText();
                if(!jsonPointer.startsWith("/"))
                {
                    throw new RuntimeException("JSON Pointer Invalid");
            }
            List<String> path = Splitter.on('/').splitToList(jsonPointer.substring(1));
            if(!isPatchAllowed(path, jsonPointer, op, clazz))
            {
                    throw new PatchNotAllowedException(clazz, op, jsonPointer);
                }
            }

            log.info("patchJson");
            log.info(patchJson.toString());

            log.info("customerJson");
            log.info(customerJson.toString());

            JsonNode resultNode = JsonPatch.apply(patchJson, customerJson);
            return objectMapper.treeToValue(resultNode, clazz);
        }
        else
        {
            throw new RuntimeException("ObjectMapper not initialized");
        }
    }

    private static boolean isPatchAllowed(List<String> path, String jsonPointer, PatchOp op, Class<?> clazz) throws NoSuchFieldException
    {
        // Check cache first
        boolean allowed = true;
        Optional<Boolean> cachedResult = checkedGet(() -> cache.get(clazz).get(jsonPointer).get(op));
        if(!cachedResult.isPresent())
        {
            // Traverse all pointer reference tokens and ensures the targeted field is
            // annotated with @AllowedPatches and the current operation is allowed.
            //JSON Pointers encode ~ as ~0 and / as ~1
            Field targetField = clazz.getField(path.get(0).replace("~0", "~").replace("~1", "/"));
            for(int i = 1; i < path.size(); i++)
            {
                targetField = targetField.getType().getField(path.get(i));
            }
            AllowedPatches allowedPatches = targetField.getAnnotation(AllowedPatches.class);
            boolean res = allowedPatches != null && Arrays.asList(allowedPatches.value()).contains(op);

            //Update the cache with the found value.
            cache.compute(clazz, (k, v) ->
            {
                Map<String, EnumMap<PatchOp, Boolean>> val = Optional.ofNullable(v).orElse(new HashMap<>());
                val.compute(jsonPointer, (k2, v2) ->
                {
                    EnumMap<PatchOp, Boolean> val2 = Optional.ofNullable(v2).orElse(new EnumMap<>(PatchOp.class));
                    val2.put(op, res);
                    return val2;
                });
                return val;
            });
            return res;
        }
        else
        {
            return cachedResult.get();
        }
    }

    /**
     * Returns an optional of the result of <code>supplier</code>, or an empty optional if the method throws
     * a {@link NullPointerException}.
     * @param supplier The function to wrap
     * @param <T> Type of object to return optionally
     * @return An optional of the result, or an empty optional.
     */
    private static <T> Optional<T> checkedGet(NullableSupplier<T> supplier)
    {
        try
        {
            return Optional.ofNullable(supplier.get());
        }
        catch(NullPointerException e)
        {
            return Optional.empty();
        }
    }

    @FunctionalInterface
    private interface NullableSupplier<T>
    {
        T get() throws NullPointerException;
    }
}
