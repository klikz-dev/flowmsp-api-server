package com.flowmsp.db;

import com.flowmsp.cache.RedisCache;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.service.MongoUtil;
import com.mongodb.client.MongoDatabase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public class PasswordDaoTest {
    private static PasswordDao passwordDao;

    @BeforeClass
    public static void beforeClass() {
        MongoDatabase  mongoDatabase    = MongoUtil.initializeMongo("mongodb://192.168.57.101:27017","FlowMSP");

        passwordDao = new PasswordDao(mongoDatabase, new RedisCache());
    }

    @Test
    public void testGetAll() {
        List<Password> passwords = passwordDao.getAll();
        Assert.assertNotNull(passwords);
    }

    @Test
    public void testGet() {
        Optional<Password> password = passwordDao.getByUsername("admin@demofd.com");
        Assert.assertTrue(password.isPresent());
    }

    @Test
    public void testGetNoMatch() {
        Optional<Password> password = passwordDao.getByUsername("bad_user_name");
        Assert.assertFalse(password.isPresent());

    }
}
