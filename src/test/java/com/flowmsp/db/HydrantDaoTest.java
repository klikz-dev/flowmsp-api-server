package com.flowmsp.db;

import com.flowmsp.SlugContext;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.service.MongoUtil;
import com.mongodb.client.MongoDatabase;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@RunWith(JUnit4.class)
public class HydrantDaoTest {
    private static HydrantDao hydrantDao;

    @BeforeClass
    public static void beforeClass() {
        MongoDatabase mongoDatabase    = MongoUtil.initializeMongo("mongodb://192.168.57.101:27017","FlowMSP");

        hydrantDao = new HydrantDao(mongoDatabase);
    }

    @Before
    public void before() {
        SlugContext.setSlug("demofd");
    }

    @After
    public void after() {
        SlugContext.clearSlug();
    }

    @Test
    public void testGetAll() {
        List<Hydrant> hydrants = hydrantDao.getAll();
        Assert.assertNotNull(hydrants);
    }
}
