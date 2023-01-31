package com.flowmsp.db;

import com.flowmsp.SlugContext;
import com.google.common.base.Strings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import com.mongodb.operation.OrderBy;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.*;


public abstract class BaseDao<T>
{
    private static final Logger log = LoggerFactory.getLogger(BaseDao.class);

    private final MongoDatabase database;
    private final Class<T>      clazz;
    private final boolean       requiresSlug;

    BaseDao(MongoDatabase database, Class<T> clazz, boolean requiresSlug) {
        this.database     = database;
        this.clazz        = clazz;
        this.requiresSlug = requiresSlug;
    }

    BaseDao(MongoDatabase database, Class<T> clazz) {
        this(database, clazz, true);
    }

    /**
     * The slug is set by the thread, usually the http server processing thread or by a worker pool thread.
     * The global SlugContext thread-local variable maintains this value. This helper function is used to
     * retrieve this value. A slug is not always present as not all calls are made in the context of a customer,
     * this call checks the flag set in every Dao and throws an exception when a slug is required by the Dao and
     * is not provided by the calling thread.
     *
     * @return  The currently active slug for the thread.
     * @throws  RuntimeException if a slug is required by the Dao but has not been provided by the calling thread.
     */
    String getSlug() {
        Optional<String> slug = SlugContext.getSlug();
        if(slug.isPresent()) {
            return slug.get();
        }
        else if (!requiresSlug) {
            return null;
        }
        else {
            log.error("Slug expected but not present");
            throw new RuntimeException("Slug expected but not present");
        }
    }

    /**
     * Each Dao must provide the collection name for the database operations as some Dao classes write to
     * global collections and others write to customer specific collections. Since many collections require
     * a slug as part of the collection name, the slug is provided so it can be used if required to build
     * the collection name.
     *
     * @param slug  The currently active slug for the thread.
     * @return      The collection name for the Dao, based on the active slug
     */
    abstract String getCollectionName(String slug);

    /**
     * Helper method to get the collection name from the Dao instance.
     *
     * @return      The collection name for the Dao
     */
    private String getCollectionName() {
        return getCollectionName(getSlug());
    }

    /**
     * Helper method to get a collection to use for all operations
     * @return
     */
    public MongoCollection<T> getCollection() {
        return database.getCollection(getCollectionName(), clazz);
    }
    
    
    /**
     * Save an entity to the collection
     *
     * @param t    The entity to save
     */
    public void save(T t) {
        getCollection().insertOne(t);
    }

    public void replaceById(String id, T t) { getCollection().replaceOne(eq("_id", id), t); }

    /**
     * Get all of the entities.
     *
     * @return The list of entities is created in its entirety before being returned, so queries
     *         that have a lot of data may require significant amounts of memory.
     */
    public List<T> getAll() {
        List<T> results = new ArrayList<>();
        getCollection().find().into(results);
        return results;
    }
    
    /**
     * Get all of the entities.
     *
     * @return The list of entities is created in its entirety before being returned, so queries
     *         that have a lot of data may require significant amounts of memory.
     */
    public List<T> getAll(String... fieldNames) {
        List<T> results = new ArrayList<>();
        getCollection().find().projection(Projections.fields(Projections.include(fieldNames))).into(results);        
        return results;
    }

    /**
     *  Get all of the entities, limiting the number of entities that are returned.
     *
     * @param limit Specifies the number of entities that will be returned. There is no ordering
     *              specified for the entities that are returned.
     * @return      The list of entities.
     */
    public List<T> getAll(int limit) {
        List<T> results = new ArrayList<>();
        Consumer<T> arrayAdd = t -> results.add(t); // Ugliness required due to MongoDriver making forEach ambiguous and anonymous lambdas
        getCollection().find().limit(limit).forEach(arrayAdd);
        return results;
    }

    /**
     *  Get all of the entities, limiting the number of entities that are returned.
     *
     * @param limit Specifies the number of entities that will be returned. There is ordering on sequence
     *              specified for the entities that are returned.
     * @return      The list of entities.
     */
    public List<T> getAllSort(int limit) {
        List<T> results = new ArrayList<>();
        Consumer<T> arrayAdd = t -> results.add(t); // Ugliness required due to MongoDriver making forEach ambiguous and anonymous lambdas
        //db.yourcollectionname.find({$query: {}, $orderby: {$natural : -1}}).limit(yournumber)
        getCollection().find().sort(Sorts.orderBy(Sorts.descending("sequence"))).limit(limit).forEach(arrayAdd);
        return results;
    }
    
    /**
     * Get the entity with the given id.
     */
    public Optional<T> getById(String id) {
        if(Strings.isNullOrEmpty(id)) { return Optional.empty(); }
        return Optional.ofNullable(getCollection().find(eq("_id", id)).first());
    }

    /**
     * Get a single entity with the given field value
     *
     * @param field     The field in the collection to filter by
     * @param value     The value of the field to filter by
     * @return          The entity matching the criteria if it exists
     */
    public Optional<T> getByFieldValue(String field, Object value) {
        if(Strings.isNullOrEmpty(field)) { return Optional.empty(); }
        return Optional.ofNullable(getCollection().find(eq(field, value)).first());
    }

    public List<T> getAllByFieldValue(String field, Object value) {
        return getAllByFilter(eq(field, value));
    }

    public List<T> getAllByFilter(Bson filter) {
        List<T> results = new ArrayList<>();
        Consumer<T> arrayAdd = t -> results.add(t);         // Ugliness required due to MongoDriver making forEach ambiguous and anonymous lambdas
        getCollection().find(filter).forEach(arrayAdd);
        return results;
    }

    public List<T> getAllSortByFilter(Bson filter, int limit) {
        List<T> results = new ArrayList<>();
        Consumer<T> arrayAdd = t -> results.add(t);         // Ugliness required due to MongoDriver making forEach ambiguous and anonymous lambdas
        getCollection().find(filter).sort(Sorts.orderBy(Sorts.descending("sequence"))).limit(limit).forEach(arrayAdd);
        return results;
    }

    public void updateById(String id, Bson updates) {
        getCollection().updateOne(eq("_id", id), updates);
    }

    public void updateAllByFieldValue(String field, Object value, Bson updates) {
        getCollection().updateMany(eq(field, value), updates);

    }
    
    /** Updates all documents in the collection with the provided updates
     *  @param updates
     */
    public void updateAll(Bson updates) {
        getCollection().updateMany(new Document(), updates);
    }

    /**
     * Deletes an entity, given its ID
     */
    public void deleteById(String id) {
        getCollection().deleteOne(eq("_id", id));
    }

    public void deleteAllByFieldValue(String field, Object value) {
        getCollection().deleteMany(eq(field, value));
    }
    
    /**
     * Deletes all entities in the Collection
     */    
    
    public void deleteAll() {
        getCollection().deleteMany(new Document());
    }

    public void createCollection() {
        database.createCollection(getCollectionName());
        createIndexes(getCollection());
    }

    protected void createIndexes(MongoCollection<T> collection) {
        collection.createIndex(Indexes.ascending("_id"));
    }

    //Get Kount
    public long getKount() {
    	return getCollection().count();
    }
    
  //Get Kount
    public long getKount(Bson filter) {
    	return getCollection().count(filter);
    }

    public List<Document> aggregate(Bson filter, String from, String localField, String foreignField, String as){
        List<Document> results = new ArrayList<>();
        Consumer<Document> arrayAdd = t -> results.add(t);         // Ugliness required due to MongoDriver making forEach ambiguous and anonymous lambdas

        database.getCollection(getCollectionName()).aggregate(
                Arrays.asList(
                        Aggregates.match(filter),
                        Aggregates.lookup(from, localField, foreignField, as)
                )
        ).forEach(arrayAdd);

        return results;
    }

}
