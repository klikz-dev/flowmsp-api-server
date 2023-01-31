package com.flowmsp;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flowmsp.cache.RedisCache;
import com.flowmsp.controller.GreetingController;
import com.flowmsp.controller.messages.MsgReceiverController;
import com.flowmsp.controller.messages.MsgSender;
import com.flowmsp.controller.auth.AuthTokenController;
import com.flowmsp.controller.customer.CustomerController;
import com.flowmsp.controller.debugPanel.debugPanelController;
import com.flowmsp.controller.email.EmailController;
import com.flowmsp.controller.partners.PartnersController;
import com.flowmsp.controller.hydrant.HydrantController;
import com.flowmsp.controller.location.LocationController;
import com.flowmsp.controller.preplan.PreplanController;
import com.flowmsp.controller.psap.PsapController;
import com.flowmsp.controller.user.UserController;
import com.flowmsp.db.*;
import com.flowmsp.domain.auth.AuthResultCode;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.License;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.user.User;
import com.flowmsp.jackson.PointDeserializer;
import com.flowmsp.jackson.PointSerializer;
import com.flowmsp.jackson.PolygonDeserializer;
import com.flowmsp.jackson.PolygonSerializer;
import com.flowmsp.service.*;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.debugpanel.GeneralService;
import com.flowmsp.service.image.ImageService;
import com.flowmsp.service.partners.partnersService;
import com.flowmsp.service.patch.PatchUtil;
import com.flowmsp.service.preplan.PreplanService;
import com.flowmsp.service.psap.PSAPService;
import com.flowmsp.service.pubsub.googlecredentials;
import com.flowmsp.service.pubsub.googlepubsub;
import com.flowmsp.service.pubsub.googlepulltimer;
import com.flowmsp.service.pubsub.googlewatchtimer;
import com.flowmsp.service.signup.SignupService;
import com.flowmsp.service.user.UserService;
import com.google.common.base.Strings;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import io.lettuce.core.RedisClient;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.mindrot.jbcrypt.BCrypt;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.sparkjava.DefaultHttpActionAdapter;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static spark.Spark.*;

public class ApiServer {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private static FreeMarkerEngine freeMarkerEngine;
    public  static Configuration    freeMarkerConfiguration;
    private static RedisClient      redisClient;

    static
    {
        freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_23);
        freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(ApiServer.class, "/templates"));
        freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);
    }

    private static Runnable shutdownFn = () -> {
        log.info("Initiating shutdown processes");
        if(redisClient != null) {
            log.info("Shutting down RedisClient");
            redisClient.shutdown();
        }
    };
    
    @SuppressWarnings("Duplicates") //IntelliJ complained that the hydrant and location endpoints were the same
    public static void main(String[] args) {
    	Runtime.getRuntime().addShutdownHook(new Thread(shutdownFn));
    	
        //Load environment variables to ApiServerConfig
        Optional.ofNullable(System.getenv("MONGO_DB"        )).ifPresent((db)     -> ApiServerConfig.databaseName   = db);
        Optional.ofNullable(System.getenv("MONGO_URI"       )).ifPresent((uri)    -> ApiServerConfig.mongoURI       = uri);
        Optional.ofNullable(System.getenv("S3_REGION"       )).ifPresent((region) -> ApiServerConfig.s3Region       = region);
        Optional.ofNullable(System.getenv("S3_URL_ROOT"     )).ifPresent((url)    -> ApiServerConfig.s3UrlRoot      = url);
        Optional.ofNullable(System.getenv("S3_IMAGE_BUCKET" )).ifPresent((bucket) -> ApiServerConfig.s3ImageBucket  = bucket);
        Optional.ofNullable(System.getenv("SNS_REGION"      )).ifPresent((region) -> ApiServerConfig.snsRegion      = region);
        Optional.ofNullable(System.getenv("SES_REGION"      )).ifPresent((region) -> ApiServerConfig.sesRegion      = region);
        Optional.ofNullable(System.getenv("SES_SYSTEM_EMAIL")).ifPresent((email)  -> ApiServerConfig.sesSystemEmail = email);
        Optional.ofNullable(System.getenv("REDIS_URI"       )).ifPresent((uri)    -> ApiServerConfig.redisURI       = uri);
        
        Optional.ofNullable(System.getenv("CLIENT_ID"       )).ifPresent((clientID)     -> ApiServerConfig.clientID   	= clientID);
        Optional.ofNullable(System.getenv("CLIENT_SECRET"   )).ifPresent((clientSecret) -> ApiServerConfig.clientSecret	= clientSecret);
        Optional.ofNullable(System.getenv("REFRESH_TOKEN"   )).ifPresent((refreshToken) -> ApiServerConfig.refreshToken = refreshToken);
        Optional.ofNullable(System.getenv("TOPIC_NAME"      )).ifPresent((topicName)    -> ApiServerConfig.topicName    = topicName);
        Optional.ofNullable(System.getenv("GOOGLE_NOTIFICATION_METHOD")).ifPresent((googleNotificationPullOrPush)    -> ApiServerConfig.googleNotificationPullOrPush  = googleNotificationPullOrPush);
        Optional.ofNullable(System.getenv("GOOGLE_MAP_API_KEY")).ifPresent((googleMapAPIKey)    -> ApiServerConfig.googleMapAPIKey  = googleMapAPIKey);
        
        Optional.ofNullable(System.getenv("PULL_TIMER")).ifPresent((pullTimer)    -> ApiServerConfig.pullTimer  =  Integer.parseInt(pullTimer));
        Optional.ofNullable(System.getenv("WATCH_TIMER")).ifPresent((watchTimer)    -> ApiServerConfig.watchTimer  = Integer.parseInt(watchTimer));

        int otbuVersion = 2;
        
        //Check if parameter value is non-sense
        if (ApiServerConfig.pullTimer < 2) {
        	//If this is less than 2 minutes, it can cause serious damage
        	ApiServerConfig.pullTimer = 2;
        }
        if (ApiServerConfig.watchTimer < 720) {
        	//If this is less than 720 minutes i.e, 12 Hours, it can cause serious damage
        	ApiServerConfig.pullTimer = 720;
        }
        // Print the startup banner
        StartupBanner.printBannerFromResource("/banner.txt");

        // Log the configuration
        ApiServerConfig.logConfiguration();

        // Initialize AWS services. For a development environment, the AWS credentials
        // must be given through environment variables. When running on AWS, the
        // credentials are automatically available through metadata.

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(ApiServerConfig.s3Region))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        AmazonSimpleEmailService ses = AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(Regions.fromName(ApiServerConfig.sesRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        AmazonSNS sns = AmazonSNSClientBuilder.standard()
                .withRegion(Regions.fromName(ApiServerConfig.snsRegion))
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();

        // Initialize Google cloud Pub/Sub. For a development environment, the Google token variables
        // and topic, project must be provided through environment variable. On AWS, these variables
        // will be provided through metadata.

	var SKIP_DISPATCH_HANDLING = System.getenv("SKIP_DISPATCH_HANDLING");
	var skip_dispatch = (SKIP_DISPATCH_HANDLING == null || SKIP_DISPATCH_HANDLING.equalsIgnoreCase("N") ? false : true);

	log.info("SKIP_DISPATCH_HANDLING=" + SKIP_DISPATCH_HANDLING);
	log.info("skip_dispatch=" + skip_dispatch);
	
	googlecredentials credentials = new googlecredentials();
	credentials.clientID = ApiServerConfig.clientID;
	credentials.clientSecret = ApiServerConfig.clientSecret;
	credentials.refreshToken = ApiServerConfig.refreshToken;
	credentials.topicName = ApiServerConfig.topicName;
	googlepubsub.GetMyInstance(credentials).Initialize();

        // Initialize the common Jackson object mapper
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Point.class, new PointSerializer());
        module.addDeserializer(Point.class, new PointDeserializer());
        module.addSerializer(Polygon.class, new PolygonSerializer());
        module.addDeserializer(Polygon.class, new PolygonDeserializer());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        mapper.setDateFormat(df);
        mapper.registerModule(module);

        // Initialize Mongo. Since the database structure is a collection-per-customer design, we will have a single
        // datastore attached to a single database. All collections will be in that database. The customer and
        // password collections will be global, all other collections will be stored per customer with the customer
        // slug used as the collection prefix.
        MongoDatabase mongoDatabase = MongoUtil.initializeMongo(ApiServerConfig.mongoURI, ApiServerConfig.databaseName);

        // Initialize Redis
        RedisCache  redisCache;
        if(Strings.isNullOrEmpty(ApiServerConfig.redisURI)) {
            // There is no redis uri, so we don't initialize the redis client
            redisClient = null;
            redisCache  = new RedisCache();
        }
        else {
            log.info("Initializing RedisClient on uri {}", ApiServerConfig.redisURI);
            redisClient = RedisClient.create(ApiServerConfig.redisURI);
            redisCache  = new RedisCache(redisClient, mapper);
        }

        // Create all of the Dao classes
        AuthLogDao              authLogDao              = new AuthLogDao(mongoDatabase);
        CustomerDao             customerDao             = new CustomerDao(mongoDatabase);
        HydrantDao              hydrantDao              = new HydrantDao(mongoDatabase);
        LocationDao             locationDao             = new LocationDao(mongoDatabase);
        PasswordDao             passwordDao             = new PasswordDao(mongoDatabase, redisCache);
        PasswordResetRequestDao passwordResetRequestDao = new PasswordResetRequestDao(mongoDatabase);
        UserDao                 userDao                 = new UserDao(mongoDatabase);
        PSAPDao                 psapDao                 = new PSAPDao(mongoDatabase);
        PsapUnitCustomerDao     psapUnitCustomerDao     = new PsapUnitCustomerDao(mongoDatabase);
        MessageDao		messageDao		= new MessageDao(mongoDatabase);
        DebugInfoDao		debugInfoDao		= new DebugInfoDao(mongoDatabase);
        PartnersDao		partnersDao		= new PartnersDao(mongoDatabase);
        OTBUDao			otbuDao			= new OTBUDao(mongoDatabase);
        DispatchRegisterDao	dispatchRegisterDao	= new DispatchRegisterDao(mongoDatabase);
        RegistrationLinkDao	registrationLinkDao	= new RegistrationLinkDao(mongoDatabase);

        DispatchReadStatusDao dispatchReadStatusDao = new DispatchReadStatusDao(mongoDatabase);
        FcmDataDao fcmDataDao = new FcmDataDao(mongoDatabase);

        // Initialize utility classes with an initialization requirement
        PatchUtil.intitialize(mapper);

        // Create all of the Service classes
        ActivityLoggingService activityLoggingService = new ActivityLoggingService(authLogDao);
        UserService    userService    = new UserService(passwordDao, passwordResetRequestDao, userDao, registrationLinkDao, ses, ApiServerConfig.sesSystemEmail, ApiServerConfig.s3UrlRoot, ApiServerConfig.s3ImageBucket);
        SignupService signupService   = new SignupService(customerDao, passwordDao, hydrantDao, locationDao, userDao, messageDao, userService, sns, ses, ApiServerConfig.sesSystemEmail, ApiServerConfig.googleMapAPIKey);
        HydrantService hydrantService = new HydrantService(hydrantDao, locationDao);
        ImageService imageService     = new ImageService(s3, ApiServerConfig.s3UrlRoot, ApiServerConfig.s3ImageBucket, locationDao);
        PSAPService psapService = new PSAPService(customerDao, psapDao, psapUnitCustomerDao, fcmDataDao, mapper);
        FCMService fcmService = new FCMService(psapService, userDao, fcmDataDao, mapper);
        MessageService messageService = new MessageService(customerDao, messageDao, locationDao, debugInfoDao, dispatchReadStatusDao, psapService, fcmService, mapper, ApiServerConfig.googleMapAPIKey);
        PreplanService preplanService = new PreplanService(hydrantDao, locationDao, messageService);
        partnersService partnersService = new partnersService(customerDao, mapper, ApiServerConfig.googleMapAPIKey);
        OneTimeBatchUpdation otbuService = new OneTimeBatchUpdation(signupService, customerDao, locationDao, otbuDao, mapper, otbuVersion);
        GeneralService generalService = new GeneralService(customerDao, userDao);
        
        // Initialize the database with both the master customer and the demo customer
        FlowMSPInitializer.initialize(customerDao, userDao, passwordDao, locationDao, signupService);
        DemoInitializer.initialize(customerDao, userDao, passwordDao, hydrantDao, locationDao, signupService, hydrantService, mapper);
        FCMInitializer.initialize();

        //Do Any Batch Updations (if Any)
        otbuService.StartUpdate();
        
        //Initialize Event handler Here
        ServerSendEventHandler.GetMyInstance(partnersDao, dispatchRegisterDao, customerDao, messageService);
        eventSource("/eventsource", ServerSendEventHandler.class);

	log.info("Reading Unread Messages from Main Thread");

	//Read Any Unread Mail for Safe Side
        List<MsgSender> unreadMsg =  messageService.readAndStoreUnreadMessages();
	for (int ii = 0; ii < unreadMsg.size(); ii ++) {
	    MsgSender msgClient = unreadMsg.get(ii);
	    try {
		ServerSendEventHandler.GetMyInstance().SendData(msgClient.customerID, mapper.writeValueAsString(msgClient), msgClient.sequence);
	    } catch (JsonProcessingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	    
	//Put Watch After Reading the Mail
	googlepubsub.GetMyInstance().WatchExpired();
	    
	//In case of Pull read messages at every 2 mintues		
	if (ApiServerConfig.googleNotificationPullOrPush.equalsIgnoreCase("Pull")) {
	    //Initialize timer for reading & saving google mail and sending them to client
	    log.info("Google PULL Notification Timer Initialized");
	    TimerTask taskReadMail = new googlepulltimer(mapper, messageService);
	    Timer timer = new Timer();
	    timer.schedule(taskReadMail, ApiServerConfig.pullTimer * 60 * 1000, ApiServerConfig.pullTimer * 60 * 1000);        	
	}
	    
	//Initialize timer for putting watch request
	//Watch is always put on while initializing the pub sub it is only refreshed here
	log.info("Google Put WATCH Timer Initialized");
	TimerTask taskPutWatch = new googlewatchtimer();
	Timer timer = new Timer();
	timer.schedule(taskPutWatch, ApiServerConfig.watchTimer * 60 * 1000 ,ApiServerConfig.watchTimer * 60 * 1000);
	
        // Configure the API security and the controllers and build the pac4j configuration
        Config pac4jConfig = initPac4j(passwordDao, userDao, customerDao, signupService, activityLoggingService);
        pac4jConfig.setHttpActionAdapter((code, context) ->
        {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("code", code);
            attrs.put("message", HttpStatus.getMessage(code));
            context.setResponseHeader("content-type", "text/html");
            halt(code, freeMarkerEngine.render(new ModelAndView(attrs, "error.ftl")));
            return null;
        });
        SecurityFilter authSecurityFilter = new SecurityFilter(pac4jConfig, "DirectBasicAuthClient");
        SecurityFilter jwtSecurityFilter  = new SecurityFilter(pac4jConfig, "HeaderClient");

        // Configure all the controller classes
        AuthTokenController authTokenController = new AuthTokenController();
        CustomerController  customerController  = new CustomerController(customerDao, dispatchRegisterDao, debugInfoDao, partnersService, partnersDao, signupService, mapper);
        GreetingController  greetingController  = new GreetingController();
        HydrantController   hydrantController   = new HydrantController(hydrantDao, customerDao, partnersDao, hydrantService, mapper, debugInfoDao);
        LocationController  locationController  = new LocationController(locationDao, hydrantDao, customerDao, userDao, messageDao, partnersDao, imageService, preplanService, messageService, mapper, debugInfoDao);
        PreplanController   preplanController   = new PreplanController(customerDao, preplanService, mapper);
        UserController      userController      = new UserController(customerDao, userDao, debugInfoDao, fcmDataDao, registrationLinkDao, userService, mapper);
        PsapController      psapController      = new PsapController(customerDao, userDao, debugInfoDao, userService, psapService, mapper);
        MsgReceiverController msgReceiverController = new MsgReceiverController(messageDao, customerDao, dispatchReadStatusDao, mapper, messageService, fcmService, psapService);
        debugPanelController debugController = new debugPanelController(generalService, debugInfoDao, mapper);
        PartnersController partnersController = new PartnersController(partnersDao, dispatchRegisterDao, partnersService, customerDao, debugInfoDao, mapper);
        // Temporary CORS stuff
        enableCORS("*", "POST, GET, PUT, PATCH, DELETE, OPTIONS, HEAD", "origin, content-type, accept, authorization, token, X-FlowMSP-Version, X-FlowMSP-Source", "true");
        
        // Configure the sparkjava routes
	EmailController emailController = new EmailController(messageDao, mapper, messageService, fcmService);
	
	path("/emailreceiver", () -> {
		post("", emailController::addMsg, mapper::writeValueAsString);
	    });
	
        path("/api", () -> {
            get("", greetingController::getGreeting, mapper::writeValueAsString);

            path("/auth/token", () -> {
                // The /api/auth/token path require basic security. If successful, the authSecurityFilter will place the
                // authenticated user information in the SparkWebContext
                before("",  authSecurityFilter);

                // Using the information in the SparkWebContext, JwtUtil will generate the JWT information, returning
                // it in a map that will be serialized to the client.
                post("", authTokenController::generateToken, mapper::writeValueAsString);

            });

            path("/signup", () -> {
                post("", customerController::addCustomer, mapper::writeValueAsString);
            });

            path("/passwordresetrequest", () -> {
                post("", userController::passwordResetRequest, mapper::writeValueAsString);
            });

            path("/completepasswordreset", () -> {
                post("", userController::completePasswordReset, mapper::writeValueAsString);
            });
            
            path("/msgreceiver", () -> {
            	get("", msgReceiverController::addSMS, mapper::writeValueAsString);
                post("", msgReceiverController::addSMS, mapper::writeValueAsString);
            });

            path("/sftpreceiver", () -> {
                get("", msgReceiverController::addSFTP, mapper::writeValueAsString);
                post("", msgReceiverController::addSFTP, mapper::writeValueAsString);
            });

            path("/customer", () -> {
                // Customer information is accessed at a global level when used for administrative tasks by anybody in
                // an organization with a MASTER license. Individuals can access their company information via a
                // subpath under their slug.
                before("",   jwtSecurityFilter);
                before("/*", jwtSecurityFilter);

                get("",       customerController::getAll,         mapper::writeValueAsString);
                get("/:id",   customerController::get,            mapper::writeValueAsString);
                patch("/:id", customerController::updateCustomer, mapper::writeValueAsString);                
            });
            

            path("/debugPanel", () -> {
            	before("",   jwtSecurityFilter);
                before("/*", jwtSecurityFilter);
                get("",    debugController::get,           mapper::writeValueAsString);
                get("/allUsersList",    debugController::getAllUsersList,           mapper::writeValueAsString);
            });
            path("/registrationLink", () -> {
                post("/:linkPart/createSecondary", userController::addUserSecondaryData, mapper::writeValueAsString);
                get("/:linkPart/validate", userController::validateRegistrationLink, mapper::writeValueAsString);
                get("/:linkPart/user", userController::getUserByRegistrationLink, mapper::writeValueAsString);
            });
            path("/:slug", () -> {
                // All paths other than /auth/token require an authenticated user presenting a valid JWT, which is
                // checked by the jwtSecurityFilter. Once authentication has been verified, the slug, which is
                // presented as the first element of the path is verified and the user checked to make sure that
                // they are authorized to access the related data. After authentication is verified, a further check
                // is done to make sure this user is has access to the organization data that they are attempting to
                // access.
                    path("/customer", () -> {
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                        get("/:id", customerController::get, mapper::writeValueAsString);
                        patch("/:id", customerController::updateCustomer, mapper::writeValueAsString);
                        patch("/:id/latlon", customerController::updateCustomerLatLon, mapper::writeValueAsString);
                        
                        // Manage the ui configuration variables
                        post("/:id/uiconfig", customerController::setUIConfig, mapper::writeValueAsString);
                        get("/:id/uiconfig", customerController::getUIConfig, mapper::writeValueAsString);
                        
                        post("/:id/registerMeForDispatch", customerController::registerMeForDispatch, mapper::writeValueAsString);
                        post("/:id/deRegisterMeForDispatch", customerController::deRegisterMeForDispatch, mapper::writeValueAsString);
                    });

                    path("/user", () -> {
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                        // Get basic data for all users in the company.
                        get("", userController::getAll, mapper::writeValueAsString);
                        post("", userController::addUser, mapper::writeValueAsString);
                        post("/createMain", userController::addUserMainData, mapper::writeValueAsString);
                        post("/resendRegistrationLink", userController::resendRegistrationLink, mapper::writeValueAsString);

                        // Get all individual user data for the specified user. You must be an admin to do this or
                        // you must be querying your own user data.
                        get("/:id", userController::get, mapper::writeValueAsString);

                        // Updates a user
                        patch("/:id", userController::updateUser, mapper::writeValueAsString);

                        // Delete a user
                        delete("/:id", userController::deleteUser, mapper::writeValueAsString);

                        // Manage the ui configuration variables
                        post("/:id/uiconfig", userController::setUIConfig, mapper::writeValueAsString);
                        get("/:id/uiconfig", userController::getUIConfig, mapper::writeValueAsString);

                        // Change the password for a user
                        post("/:id/password", userController::changePassword, mapper::writeValueAsString);
                        put("/:id/upload", userController::upload, mapper::writeValueAsString);
                    });

                    path("/hydrant", () -> {
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                        get("", hydrantController::getAll, mapper::writeValueAsString);
                        put("", hydrantController::add,    mapper::writeValueAsString);
                        delete("", hydrantController::deleteAll,        mapper::writeValueAsString);

                        get("/partners/:partnerId", hydrantController::getPartner, mapper::writeValueAsString);
                        get("/all", hydrantController::getAllPartner, mapper::writeValueAsString);

                        get("/:id",    hydrantController::get,           mapper::writeValueAsString);
                        patch("/:id",  hydrantController::updateHydrant, mapper::writeValueAsString);
                        delete("/:id", hydrantController::delete,        mapper::writeValueAsString);

                        put("/upload", hydrantController::upload, mapper::writeValueAsString);
                    });

                    path("/location", () -> {
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                        get("", locationController::getAll,      mapper::writeValueAsString);
                        put("", locationController::addLocation, mapper::writeValueAsString);
                        get("/partners/:partnerId", locationController::getPartner,      mapper::writeValueAsString);
                        get("/all", locationController::getAllPartner,      mapper::writeValueAsString);

                        get(   "/:id", locationController::get,            mapper::writeValueAsString);
                        patch( "/:id", locationController::updateLocation, mapper::writeValueAsString);
                        delete("/:id", locationController::deleteLocation, mapper::writeValueAsString);

                        get(   "/geo/:lat/:lon", locationController::getFromPosition,  mapper::writeValueAsString);
                        
                        post(  "/:id/image", locationController::uploadImage, mapper::writeValueAsString);
                        get("/:id/image/:imageId", locationController::getImage, mapper::writeValueAsString);
                        delete("/:id/image/:imageId", locationController::deleteImage, mapper::writeValueAsString);
                        put("/:id/image/:imageId/annotation", locationController::setAnnotationMetadata, mapper::writeValueAsString);
                        get("/:id/image/:imageId/annotation", locationController::getAnnotationMetadata, mapper::writeValueAsString);
                        get("/:id/image/:imageId/annotationSVG", locationController::getAnnotationMetadataSVG, mapper::writeValueAsString);
                        put("/:id/image/:imageId/tags", locationController::setTags, mapper::writeValueAsString);
                        get("/:id/image/:imageId/tags", locationController::getTags, mapper::writeValueAsString);
                        get("/:id/preplan", locationController::preplan, mapper::writeValueAsString);
                        post("/:id/imageReorder", locationController::reorderImage, mapper::writeValueAsString);
                        put("/upload", locationController::uploadPrePlan, mapper::writeValueAsString);
                    });

                    path("/preplan", () -> {
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                        post("", preplanController::preplan, mapper::writeValueAsString);
                    });
                    path("/partners", () -> {
                        // Customer information is accessed at a global level when used for administrative tasks by anybody in
                        // an organization with a MASTER license. Individuals can access their company information via a
                        // subpath under their slug.
                        before("",   jwtSecurityFilter, ApiServer::authorizeSlug);
                        before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);
                        
                        get("/:id", partnersController::getAll,         mapper::writeValueAsString);
                        put("/:id", partnersController::addPartners,     mapper::writeValueAsString);
                        get("/:id/radius/:radius", customerController::getAllRadius,         mapper::writeValueAsString);                        
                    });
                path("/dispatchfeed", () -> {
                    before("", jwtSecurityFilter, ApiServer::authorizeSlug);
                    before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                    get("", msgReceiverController::getDispatchFeed, mapper::writeValueAsString);
                    get("/badge", msgReceiverController::getBadge, mapper::writeValueAsString);
                });

                path("/pushNotifications", () -> {
                    before("", jwtSecurityFilter, ApiServer::authorizeSlug);
                    before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                    path("/registrationToken", () -> {
                        post("", userController::addRegistrationToken, mapper::writeValueAsString);
                        delete("", userController::deleteRegistrationToken, mapper::writeValueAsString);
                    });

//                    path("/test", () -> {
//                        get("", msgReceiverController::testPush, mapper::writeValueAsString);
//                    });
//
//                    path("/testPSAP", () -> {
//                        get("", msgReceiverController::testPushPSAP, mapper::writeValueAsString);
//                    });

                    path("/enable", () -> {
                        post("", userController::enableDuty, mapper::writeValueAsString);
                    });

                    path("/disable", () -> {
                        post("", userController::disableDuty, mapper::writeValueAsString);
                    });

                });

                path("/psap", () -> {
                    before("", jwtSecurityFilter, ApiServer::authorizeSlug);
                    before("/*", jwtSecurityFilter, ApiServer::authorizeSlug);

                    path("/units", () -> {
                        get("", psapController::getUserUnits, mapper::writeValueAsString);
                        post("", psapController::setUserUnits, mapper::writeValueAsString);
                    });
                });
            });
            path("/except", () ->
            {
                get("", (req, res) ->
                {
                    throw new RuntimeException();
                });
            });

            after("", (req, res) -> {
            	res.type("application/json");
            	res.header("Content-Encoding", "gzip");
            });
            after("/*",(req,res) -> {
            	res.type("application/json");
            	res.header("Content-Encoding", "gzip");
            });

            afterAfter("/*", (req,res) -> SlugContext.clearSlug());
        });
        notFound((req, res) ->
        {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("code", res.status());
            attrs.put("message", HttpStatus.getMessage(res.status()));

            res.header("content-type", "text/html");
            return freeMarkerEngine.render(new ModelAndView(attrs, "error.ftl"));
        });
        exception(Exception.class, (exc, req, res) ->
        {
            Map<String, Object> attrs = new HashMap<>();
            res.status(500);

            attrs.put("code", res.status());
            attrs.put("exception", exc.toString());
            attrs.put("traces", Arrays.asList(exc.getStackTrace()));

            res.header("content-type", "text/html");
            res.body(freeMarkerEngine.render(new ModelAndView(attrs, "exception.ftl")));
        });
    }

    private static Config initPac4j(PasswordDao passwordDao, UserDao userDao, CustomerDao customerDao, SignupService signupService, ActivityLoggingService activityLoggingService) {
        List<Client> clients = new ArrayList<>();

        clients.add(new DirectBasicAuthClient((credentials, context) -> ApiServer.authenticateUserPassword(passwordDao, userDao, customerDao, signupService, activityLoggingService, credentials, (SparkWebContext)context)));
        clients.add( new HeaderClient("Authorization", JwtUtil::validate));

        Config config = new Config(clients);
        config.setHttpActionAdapter(new DefaultHttpActionAdapter());

        return config;
    }

    /*
     * When a user attempts to authenticate via the /auth/token path, the DirectBasicAuthClient is configured to call
     * this method. It will verify that the password is valid for the user and if so, it will create a pac4j profile.
     * The profile will contain the user id, email, customer id and customer slug. If the password does not validate
     * then no profile information will be set and the security filter will fail.
     */
    private static void authenticateUserPassword(PasswordDao passwordDao, UserDao userDao, CustomerDao customerDao, SignupService signupService, ActivityLoggingService activityLoggingService,
                                                 Credentials credentials, SparkWebContext context) throws HttpAction, CredentialsException {
        log.debug("Authenticating user password");
        // Extract the email and password from the credentials.
        UsernamePasswordCredentials upc = (UsernamePasswordCredentials)credentials;
        String providedUsername = upc.getUsername().toLowerCase();
        String providedPassword = upc.getPassword();
        log.debug("Extracted {} from credentials", providedUsername);

        // Because the slug is not set, this filter is initially limited to accessing only those Dao classes that
        // do not require a slug: passwordDao, customerDao, etc.
        Optional<Password> p = passwordDao.getByUsername(providedUsername);

        if(p.isPresent()) {
            // Determine if the provided password matches the hashed password in the database
            if(BCrypt.checkpw(providedPassword, p.get().password)) {
                final CommonProfile profile = new CommonProfile();
                profile.setId(p.get().id);
                profile.addAttribute(Pac4jConstants.USERNAME, p.get().username);
                profile.addAttribute("customerId", p.get().customerId);
                profile.addAttribute("slug", p.get().customerSlug);

                // Since we have a slug, the SlugContext can be updated and any Dao can be accessed at this point
                SlugContext.setSlug(p.get().customerSlug);
                Optional<User> user = userDao.getById(p.get().id);
                user.ifPresent(user1 -> profile.addRole(user1.role.toString()));                

                // Log the login
                activityLoggingService.logSuccessfulLogin(providedUsername, context.getRemoteAddr(), p.get().customerSlug);

                // And finally check to make sure the license is valid, otherwise send an email if not.
                try {
                    Optional<Customer> customer = customerDao.getById(user.get().customerRef.customerId);
                    if (customer.isPresent()) {
                        License license = customer.get().license;
                        profile.addAttribute("licenseType", license.licenseType.toString());
                        if (license != null && license.licenseType != null && license.expirationTimestamp != null) {
                            if (license.licenseType != LicenseType.Master && license.expirationTimestamp.before(new Date())) {
                                log.info("User {} license has expired", user.get().email);
                                profile.addAttribute("msg", "Your license has expired.");
                                signupService.notifyExpiredLicense(user.get(), customer.get(), license);
                            }
                        } else if (license != null && license.licenseType != null && license.licenseType != LicenseType.Master && license.expirationTimestamp == null) {
                            log.info("Setting expiration date for {} to 2018-01-18");
                            Customer c = customer.get();
                            c.license.expirationTimestamp = Date.from(LocalDateTime.of(2018, 1, 18, 0, 0, 0, 0).toInstant(ZoneOffset.UTC));
                            customerDao.replaceById(c.id, c);
                        }
                    }
                    else {
                        log.error("Unexpected error attempting customer to do a license check");
                    }                    
                }
                catch(Exception e) {
                    log.error("Error with license check", e);
                }
                
                credentials.setUserProfile(profile);
                SlugContext.clearSlug();
            }
            else {
                // The password was not correct, so log and abort
                activityLoggingService.logUnsuccssfulLogin(providedUsername, context.getRemoteAddr(), AuthResultCode.INVALID_PASSWORD);
            }
        }
        else {
            // The user didn't exist in the database, so abort the process right here after logging
            // the attempt
            activityLoggingService.logUnsuccssfulLogin(providedUsername, context.getRemoteAddr(), AuthResultCode.INVALID_USER);
        }
    }

    /*
     * Verifies that the slug in the URL path being accessed is one that the current user is authorized to access. If
     * the authorization succeeds, the SlugContext is set for this thread.
     */
    private static void authorizeSlug(Request req, Response res) {
        final SparkWebContext context               = new SparkWebContext(req, res);
        final ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
        final Optional<CommonProfile> profile       = manager.get(false);

        if(profile.isPresent()) {
            String pathSlug = req.params("slug");
            String userSlug =(String)profile.get().getAttribute("slug");
            String licenseType = (String)profile.get().getAttribute("licenseType");
            if(pathSlug.equals(userSlug) || (licenseType != null && licenseType.equals(LicenseType.Master.toString()))) {
                SlugContext.setSlug(userSlug);
            }
            else {
                halt(403);
            }
        }
        else {
            halt(401);
        }
    }

    //Temporary CORS stuff
    private static void enableCORS(final String origin, final String methods, final String headers, String allowCredentials) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin",      origin);
            response.header("Access-Control-Allow-Methods",     methods);
            response.header("Access-Control-Allow-Headers",     headers);
            response.header("Access-Control-Allow-Credentials", allowCredentials);
            response.type("application/json");

            // If it is options, then halt with no content
            String method = request.requestMethod();
            if(method.equals(HttpMethod.OPTIONS.toString())) {
                halt(HttpStatus.NO_CONTENT_204);
            }

        });
    }
}
