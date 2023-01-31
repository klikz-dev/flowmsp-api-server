package com.flowmsp.service;

import com.flowmsp.db.AuthLogDao;
import com.flowmsp.domain.auth.AuthAttempt;
import com.flowmsp.domain.auth.AuthLog;
import com.flowmsp.domain.auth.AuthResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;

import static com.mongodb.client.model.Updates.push;

public class ActivityLoggingService
{
    private static final Logger log = LoggerFactory.getLogger(ActivityLoggingService.class);

    private final AuthLogDao authLogDao;

    public ActivityLoggingService(AuthLogDao authLogDao) {
        this.authLogDao = authLogDao;
    }

    public void logSuccessfulLogin(String username, String remoteAddr, String companySlug)
    {
        log.debug("Successful login from {} at {}", username, remoteAddr);
        AuthAttempt aa = new AuthAttempt();
        aa.timestamp = ZonedDateTime.now();
        aa.username  = username;
        aa.remoteAddr = remoteAddr;
        aa.companySlug = companySlug;
        aa.resultCode = AuthResultCode.SUCCESS;
        saveAuthAttempt(aa);
    }

    public void logUnsuccssfulLogin(String username, String remoteAddr, AuthResultCode authResultCode)
    {
        log.debug("Unsuccessful login from {} at {}", username, remoteAddr);
        AuthAttempt aa = new AuthAttempt();
        aa.timestamp = ZonedDateTime.now();
        aa.username  = username;
        aa.remoteAddr = remoteAddr;
        aa.resultCode = authResultCode;
        saveAuthAttempt(aa);
    }

    private ZonedDateTime cachedTimestampHour = null;
    private void saveAuthAttempt(AuthAttempt authAttempt) {
        // Get the timestamp hour for the attempt
        ZonedDateTime timestampHour = authAttempt.timestamp.withMinute(0)
                                                           .withSecond(0)
                                                           .withNano(0);

        // Check the cached timestamp to see if it is the same as one being persisted, if not
        // check to see if it is in the database and just not cached, and if not create it
        if(cachedTimestampHour == null || !cachedTimestampHour.equals(timestampHour) ) {
            Optional<AuthLog> authLog = authLogDao.getByFieldValue("_id", timestampHour);
            if (authLog.isPresent()) {
                cachedTimestampHour = timestampHour;
            } else {
                AuthLog newAuthLog = new AuthLog(timestampHour);
                authLogDao.save(newAuthLog);
                cachedTimestampHour = timestampHour;
            }
        }

        // The existence of the authLog document in the database has been verified, so add
        // the auth attempt
        authLogDao.updateAllByFieldValue("_id", timestampHour, push("authAttempts", authAttempt));
    }
}
