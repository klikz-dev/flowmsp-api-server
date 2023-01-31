package com.flowmsp.db;

import com.flowmsp.domain.auth.AuthAttempt;
import com.flowmsp.domain.auth.AuthLog;
import com.flowmsp.domain.auth.AuthResultCode;
import com.flowmsp.service.MongoUtil;
import com.mongodb.client.MongoDatabase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Updates.push;

public class AuthLogDaoTest {
    private static AuthLogDao authLogDao;

    @BeforeClass
    public static void beforeClass() {
        MongoDatabase mongoDatabase    = MongoUtil.initializeMongo("mongodb://192.168.57.101:27017","FlowMSP");

        authLogDao = new AuthLogDao(mongoDatabase);
    }

    @Test
    public void testSaveAuthLog() {
        AuthLog al = new AuthLog(Instant.now().atZone(ZoneId.of("UTC")).withMinute(0).withSecond(0).withNano(0));
        authLogDao.save(al);
    }

    @Test
    public void testAddAuthAttempt() {
        ZonedDateTime timestampHour = Instant.now().atZone(ZoneId.of("UTC")).withMinute(0).withSecond(0).withNano(0);
        Optional<AuthLog> al = authLogDao.getByFieldValue("_id", timestampHour);
        if(al.isPresent()) {
            AuthAttempt aa = new AuthAttempt();
            aa.timestamp   = Instant.now().atZone(ZoneId.of("UTC"));
            aa.remoteAddr  = "0.0.0.0";
            aa.resultCode  = AuthResultCode.SUCCESS;
            aa.username    = "testuser@test.com";
            aa.companySlug = "slugo";
            authLogDao.updateAllByFieldValue("_id", timestampHour, push("authAttempts", aa));
        }
    }

    @Test
    public void testGetAll() {
        List<AuthLog> logs = authLogDao.getAll();
        Assert.assertNotNull(logs);
    }
}
