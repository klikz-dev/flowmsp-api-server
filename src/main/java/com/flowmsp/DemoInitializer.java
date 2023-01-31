package com.flowmsp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.*;
import com.flowmsp.domain.*;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.License;
import com.flowmsp.domain.customer.LicenseTerm;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.location.Location;
import com.flowmsp.domain.user.User;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.service.FlowService;
import com.flowmsp.service.HydrantService;
import com.flowmsp.service.signup.SignupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

class DemoInitializer {
    private static final Logger log = LoggerFactory.getLogger(DemoInitializer.class);

    private static final String demoCompanyId   = "f9301c47-d7bf-4526-9e30-888ee5014f9c";
    private static final String demoCompanyName = "Demo Fire Department";
    private static final String demoSlug        = "demofd";
    private static final String demoUserId      = "bd8ccedb-08df-4498-90e8-a84e6e5dfb3b";
    private static final String demoEmail       = "admin@demofd.com";
    private static final String demoPassword    = "demofdAdmin";
    private static final String demoFirstName   = "DemoFD";
    private static final String demoLastName    = "Administrator";

    static void initialize(CustomerDao customerDao, UserDao userDao, PasswordDao passwordDao, HydrantDao hydrantDao, LocationDao locationDao, SignupService signupService, HydrantService hydrantService, ObjectMapper mapper) {
        // The SlugContext must be set as the User collection, which is updated here, is dependent on it.
        SlugContext.setSlug(demoSlug);

        Optional<Customer> masterCustomer = customerDao.getById(demoCompanyId);
        if(!masterCustomer.isPresent()) {
            // The master customer doesn't exist so create it and the master user
            log.info("Initializing the demo customer");

            signupService.createCollections();

            // Create the customer
            log.info("Loading demo customers");
            License license  = new License(LicenseType.Demo, LicenseTerm.Perpetual);
            Address  address  = new Address("100 Main St.", null, "Ottawa", "IL", "61350", 41.35, -88.84);
            Customer customer = new Customer(demoCompanyId, demoSlug, demoCompanyName, address, license);
            customerDao.save(customer);

            // Create the user
            log.info("Loading demo users");
            User user = new User(demoUserId, demoEmail, demoFirstName, demoLastName, UserRole.ADMIN, customer);
            userDao.save(user);

            // Create the password
            log.info("Loading demo passwords");
            Password password = new Password(user, Password.encryptPassword(demoPassword), customer);
            passwordDao.save(password);

            //load hydrants form file
            log.info("Loading demo hydrants");
            try {
                List<Hydrant> hydrants = mapper.readValue(DemoInitializer.class.getResourceAsStream("/demo/flow-msp-hydrants.json"), new TypeReference<List<Hydrant>>() {});
                hydrants.forEach((hydrant -> {
                    hydrant.customerId = customer.id;
                    hydrant.customerSlug = customer.slug;
                    hydrantService.setHydrantFlowRange(hydrant, customer);
                    hydrantDao.save(hydrant);
                }));
            }
            catch(IOException e) {
                log.error("Error loading demo hydrants", e);
            }

            //load locations from file
            log.info("Loading demo locations");
            try
            {
                List<Location> locations = mapper.readValue(DemoInitializer.class.getResourceAsStream("/demo/flow-msp-locations.json"), new TypeReference<List<Location>>() {});
                locations.forEach((loc) -> {
                    loc.customerId   = customer.id;
                    loc.customerSlug = customer.slug;
                    loc.roofArea     = FlowService.calcArea(loc.geoOutline);
                    loc.requiredFlow = FlowService.calcRequiredFlow(loc.roofArea);
                    locationDao.save(loc);
                });
            }
            catch(IOException e)
            {
                log.error("Error loading demo locations", e);
            }
        }
        else {
            log.info("Demo customer already initialized");
        }

        // Clear the SlugContext when complete
        SlugContext.clearSlug();
    }

}

