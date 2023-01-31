package com.flowmsp.db;

import com.flowmsp.domain.customer.Customer;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonValue;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class CustomerDao extends BaseDao<Customer> {
    public CustomerDao(MongoDatabase database) {
        super(database, Customer.class, false);
    }

    @Override
    protected String getCollectionName(String slug) {
        // The customer collection is global so the slug is not prepended to the collection name
        return "Customer";
    }

    public Optional<Customer> getBySlug(String slug) {
        return getByFieldValue("slug", slug);
    }

    public Optional<Customer> addUIConfig(String id, Map<String, BsonValue> config) {
        List<Bson> updates = new ArrayList<>();
        config.forEach((key,value) -> updates.add(set("uiConfig.".concat(key), value)));
        updateById(id, combine(updates));
        return getById(id);
    }

    private static final List<String> stopwords = Lists.newArrayList("a", "an", "the", "to", "of");

    public String makeSlug(String name) {
        boolean validSlug = false;

        var clean_name = name.replaceAll("[^A-Za-z0-9 ]", "");

        if (clean_name.length() == 0)
            clean_name = "anne";

        String[] words = clean_name.split("\\s");
        var initial_slug = Arrays.stream(words)
                .map(String::toLowerCase)
                .filter(s -> !stopwords.contains(s))
                .collect(Collectors.joining())
                .concat("           ")
                .substring(0, 12)
                .trim();
        int loop = 1;
        var slug = initial_slug;

        do {
            Optional<Customer> c = getByFieldValue("slug", slug);

            if(c.isPresent()) {
                slug = initial_slug.concat(loop == 1  ? "" : Integer.toString(loop));
                ++loop;
            }
            else {
                validSlug = true;
            }
        } while (!validSlug);

        return slug;
    }
}
