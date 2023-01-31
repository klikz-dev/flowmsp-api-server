package com.flowmsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ApiServerConfig {
    private static final Logger log = LoggerFactory.getLogger(ApiServerConfig.class);

    // MongoDB configuration parameters
    static String mongoURI      = "mongodb://localhost:27017";
    static String databaseName  = "FlowMSP";

    // Amazon S3 configuration
    static String s3Region      = "us-west-2";
    static String s3UrlRoot     = "https://s3-us-west-2.amazonaws.com";
    static String s3ImageBucket = "flowmsp-test-image-bucket2";

    // Amazon SNS configuration
    static String snsRegion     = "us-west-2";

    // Amazon SES configuration
    static String sesRegion      = "us-west-2";
    static String sesSystemEmail = "admin@flowmsp.com";

    // Redis configuration
    static String redisURI      = null;
    
    //Google Authentication configuration
    static String clientID = "DEV";
    static String clientSecret = "DEV";    
    static String refreshToken = "DEV";
    static String topicName = "DEV";
    static String googleNotificationPullOrPush = "pull";
    
    static int pullTimer = 1 * 60; //In Mintues
    static int watchTimer = 24 * 60; //In Mintues
    
    static String googleMapAPIKey = "AIzaSyCx-skGzBQpfifpGsclSgQ0rlDng25ZdCg";
    // Log the configuration
    static void logConfiguration() {
        log.info("APIServerConfig");
        log.info("mongoURI      : {}", mongoURI);
        log.info("databaseName  : {}", databaseName);
        log.info("S3Region      : {}", s3Region);
        log.info("s3UrlRoot     : {}", s3UrlRoot);
        log.info("s3ImageBucket : {}", s3ImageBucket);
        log.info("snsRegion     : {}", snsRegion);
        log.info("sesRegion     : {}", sesRegion);
        log.info("sesSystemEmail: {}", sesSystemEmail);
        log.info("redisURI      : {}", redisURI);
        log.info("clientID      : {}", clientID);
        log.info("clientSecret  : {}", clientSecret);
        log.info("refreshToken  : {}", refreshToken);
        log.info("topicName     : {}", topicName);
        log.info("googleNotificationPullOrPush: {}", googleNotificationPullOrPush);
        log.info("googleMapAPIKey: {}", googleMapAPIKey);
        log.info("pullTimer: {}", pullTimer);
        log.info("watchTimer: {}", watchTimer);
    }

}
