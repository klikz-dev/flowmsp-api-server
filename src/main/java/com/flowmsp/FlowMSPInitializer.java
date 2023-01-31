package com.flowmsp;

import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.LocationDao;
import com.flowmsp.db.PasswordDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.customer.License;
import com.flowmsp.domain.customer.LicenseTerm;
import com.flowmsp.domain.customer.LicenseType;
import com.flowmsp.domain.user.User;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.service.signup.SignupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

class FlowMSPInitializer {
    private static final Logger log = LoggerFactory.getLogger(FlowMSPInitializer.class);

    private static final String flowCompanyId   = "f6dd72cd-5b7d-44ab-b34a-c8400fbe8681";
    private static final String flowCompanyName = "Flow MSP";
    private static final String flowSlug        = "flowmsp";
    private static final String flowUserId      = "99441268-a7de-4c09-84c1-cdc207f643fd";
    private static final String flowEmail       = "admin@flowmsp.com";
    private static final String flowPassword    = "flowmspAdmin";
    private static final String flowFirstName   = "FlowMSP";
    private static final String flowLastName    = "Administrator";

    static void initialize(CustomerDao customerDao, UserDao userDao, PasswordDao passwordDao, LocationDao locationDao, SignupService signupService) {
        // The SlugContext must be set as the User collection, which is updated here, is dependent on it.
        SlugContext.setSlug(flowSlug);

        Optional<Customer> masterCustomer = customerDao.getById(flowCompanyId);
        if(!masterCustomer.isPresent()) {
            // The master customer doesn't exist so create it and the master user
            log.info("Initializing the master customer");

            signupService.createCollections();

            // Create the customer
            License license = new License();
            license.licenseType = LicenseType.Master;
            license.licenseTerm = LicenseTerm.Perpetual;

            Customer customer = new Customer();
            customer.id      = flowCompanyId;
            customer.name    = flowCompanyName;
            customer.slug    = flowSlug;
            customer.license = license;
            customerDao.save(customer);

            // Create the user
            User user = new User(flowUserId, flowEmail, flowFirstName, flowLastName, UserRole.ADMIN, customer);
            userDao.save(user);

            // Create the password
            Password password = new Password(user, Password.encryptPassword(flowPassword), customer);
            passwordDao.save(password);
        }
        else {
            log.info("Master customer already initialized");
        }

        // Clear the SlugContext when complete
        SlugContext.clearSlug();
    }

}
