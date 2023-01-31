package com.flowmsp;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.resource.ClassPathResource;

import java.io.IOException;
import java.io.FileInputStream;

class FCMInitializer {

    static void initialize() {
        String firebaseConfigPath = System.getenv("FIREBASE_KEY");
        Logger logger = LoggerFactory.getLogger(FCMInitializer.class);

	logger.info("firebaseConfigPath=" + firebaseConfigPath);

        try {
	    FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);
            FirebaseOptions options = new FirebaseOptions.Builder()
		.setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase application has been initialized");
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

}
