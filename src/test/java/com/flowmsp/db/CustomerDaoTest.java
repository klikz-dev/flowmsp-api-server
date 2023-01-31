package com.flowmsp.db;

import com.flowmsp.domain.customer.Customer;
import com.flowmsp.service.MongoUtil;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDouble;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(JUnit4.class)
public class CustomerDaoTest {
    private static CustomerDao customerDao;

    @BeforeClass
    public static void beforeClass() {
        MongoDatabase  mongoDatabase    = MongoUtil.initializeMongo("mongodb://192.168.57.101:27017","FlowMSP");

        customerDao = new CustomerDao(mongoDatabase);
    }

    @Test
    public void testGetAll() {
        List<Customer> customers = customerDao.getAll();
        Assert.assertNotNull(customers);
    }

    @Test
    public void testGet() {
        Optional<Customer> c = customerDao.getBySlug("demofd");
        Assert.assertTrue(c.isPresent());
    }

    @Test
    public void testWriteUIConfig() {
        Map<String, BsonValue> m = new HashMap<>();
        m.put("stringVal", new BsonString("String"));
        m.put("doubleVal", new BsonDouble(3292.32));

        Optional<Customer> c = customerDao.getBySlug("demofd");
        if(c.isPresent()) {
            customerDao.addUIConfig(c.get().id, m);
        }
    }

}
