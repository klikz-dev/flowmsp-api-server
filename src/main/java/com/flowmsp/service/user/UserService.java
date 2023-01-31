package com.flowmsp.service.user;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.flowmsp.ApiServer;
import com.flowmsp.controller.user.UserUploadResponse;
import com.flowmsp.db.PasswordDao;
import com.flowmsp.db.PasswordResetRequestDao;
import com.flowmsp.db.RegistrationLinkDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.domain.auth.Password;
import com.flowmsp.domain.auth.RegistrationLink;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.user.PasswordResetRequest;
import com.flowmsp.domain.user.User;
import com.flowmsp.domain.user.UserRole;
import com.flowmsp.util.StringGenerator;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import freemarker.template.Template;
import org.joda.time.DateTime;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.*;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final PasswordDao passwordDao;
    private final RegistrationLinkDao registrationLinkDao;
    private final PasswordResetRequestDao passwordResetRequestDao;
    private final UserDao userDao;
    private final AmazonSimpleEmailService ses;

    private final String systemEmail;
    private final String s3UrlRoot;
    private final String imageBucket;

    public UserService(PasswordDao passwordDao, PasswordResetRequestDao passwordResetRequestDao, UserDao userDao, RegistrationLinkDao registrationLinkDao, AmazonSimpleEmailService ses, String systemEmail, String s3UrlRoot, String imageBucket) {
        this.passwordDao = passwordDao;
        this.passwordResetRequestDao = passwordResetRequestDao;
        this.userDao = userDao;
        this.registrationLinkDao = registrationLinkDao;
        this.ses = ses;
        this.systemEmail = systemEmail;
        this.s3UrlRoot = s3UrlRoot;
        this.imageBucket = imageBucket;
    }

    /**
     * Add a user after verifying that the username is unique across all users of the system. Also validates that the
     * role is valid.
     */
    public UserResult addUser(UserRequest userRequest, Customer customer) {
        // The email address is going to be used as the initial username, therefore it cannot already be used
        // as a username. If it is, we will abort processing and return with an error.
        log.info("Creating user {}", userRequest);
        Optional<Password> existingUser = passwordDao.getByUsername(userRequest.email.toLowerCase());
        if (existingUser.isPresent()) {
            log.error("Error creating user, duplicate username");
            return new UserResult("Duplicate username");
        }

        // The role must be one of the acceptable values
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(userRequest.role);
        } catch (Exception e) {
            userRole = null;
        }
        if (userRole == null) {
            log.error("Cannot create user, invalid role");
            return new UserResult("Invalid user role");
        }

        // Create the user
        User u = new User(UUID.randomUUID().toString(),
                userRequest.email.toLowerCase(),
                userRequest.firstName,
                userRequest.lastName,
                userRole,
                customer);

        // Create the password entry
        Password p = new Password(u, Password.encryptPassword(userRequest.password), customer);

        userDao.save(u);
        passwordDao.save(p);

        return new UserResult(userDao.getById(u.id).get());
    }

    /**
     * Add a user after verifying that the username is unique across all users of the system. Also validates that the
     * role is valid.
     */
    public UserResult addUserMainData(UserMainDataRequest userRequest, Customer customer) {
        // The email address is going to be used as the initial username, therefore it cannot already be used
        // as a username. If it is, we will abort processing and return with an error.
        log.info("Creating user {}", userRequest);
        Optional<Password> existingUser = passwordDao.getByUsername(userRequest.email.toLowerCase());
        if (existingUser.isPresent()) {
            log.error("Error creating user, duplicate username");
            return new UserResult("Duplicate username");
        }

        // The role must be one of the acceptable values
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(userRequest.role);
        } catch (Exception e) {
            userRole = null;
        }
        if (userRole == null) {
            log.error("Cannot create user, invalid role");
            return new UserResult("Invalid user role");
        }

        // Create the user
        User u = new User(UUID.randomUUID().toString(),
                userRequest.email.toLowerCase(),
                "",
                "",
                userRole,
                customer);

        // Create link entry

        RegistrationLink registrationLink = new RegistrationLink(
                UUID.randomUUID().toString(),
                userRequest.email.toLowerCase(),
                u.id,
                customer.id,
                customer.slug,
                StringGenerator.randomAlpha(50),
                System.currentTimeMillis(),
                false
        );

        // Create random password entry
        String randomPassword = StringGenerator.randomAlphaNumericSymbol(25);
        Password p = new Password(u, Password.encryptPassword(randomPassword), customer);

        userDao.save(u);
        passwordDao.save(p);
        registrationLinkDao.save(registrationLink);



        return new UserResult(userDao.getById(u.id).get());
    }

    /**
     * Add a user after verifying that the username is unique across all users of the system. Also validates that the
     * role is valid.
     */
    public UserResult addUserSecondaryData(UserSecondaryDataRequest userRequest, String linkPart) {
        // The email address is going to be used as the initial username, therefore it cannot already be used
        // as a username. If it is, we will abort processing and return with an error.
        log.info("Creating user {}", userRequest);
        Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByLinkPart(linkPart);
        if (registrationLinkOpt.isEmpty()) {
            log.error("Error creating secondary data");
            return new UserResult("Missing user");
        }
        RegistrationLink registrationLink = registrationLinkOpt.get();
        registrationLink.used = true;

        Optional<User> uOpt = userDao.getById(registrationLink.userId);
        if (uOpt.isEmpty()) {
            log.error("Error creating secondary data");
            return new UserResult("Missing user");
        }
        User u = uOpt.get();
        u.firstName = userRequest.firstName;
        u.lastName = userRequest.lastName;

        Optional<Password> passwordOpt = passwordDao.getByUsername(u.email);
        if (passwordOpt.isEmpty()) {
            log.error("Error creating secondary data");
            return new UserResult("Missing user");
        }
        Password p = passwordOpt.get();
        p.password = Password.encryptPassword(userRequest.password);

        userDao.replaceById(u.id, u);
        passwordDao.replaceById(p.id, p);
        registrationLinkDao.replaceById(registrationLink.id, registrationLink);

        return new UserResult(userDao.getById(u.id).get());
    }

    /**
     * Send an email with the registration link
     */
    public Optional<RegistrationLink> sendRegistrationLink(RegistrationLink registrationLink, String registerUrlFull) {

        // Send the email
        try {
            // Create the email from the FreeMarker template
            Template template = ApiServer.freeMarkerConfiguration.getTemplate("registrationLink.ftl");
            Writer stringOut = new StringWriter();

            String prefix = String.format("%s/%s/", s3UrlRoot, imageBucket);

            Map<String, Object> model = new HashMap<>();
            model.put("registrationLink", registerUrlFull);
            model.put("imagePrefix", prefix);
            template.process(model, stringOut);

            SendEmailRequest emailRequest = new SendEmailRequest();
            emailRequest.setDestination(new Destination().withToAddresses(registrationLink.username));
            emailRequest.setMessage(new Message().withSubject(new Content().withData("FlowMSP Registration Link")).withBody(new Body().withHtml(new Content().withData(stringOut.toString()))));
            emailRequest.setSource(systemEmail);

            ses.sendEmail(emailRequest);
        } catch (Exception e) {
            log.error("Error sending registration link email", e);
            //return Optional.empty();
        }

        // Return the request
        log.info("Successfully sent registration link email to {}", registrationLink.username);

        return Optional.of(registrationLink);
    }

    public boolean validateRegistrationLink(String linkPart){
        Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByLinkPart(linkPart);
        if (registrationLinkOpt.isEmpty()) {
            return false;
        }
        return !registrationLinkOpt.get().used;
    }

    /**
     * Delete a user given their user id. Also deletes their password entry so usernames can be re-used after they
     * are delete.
     */
    public void deleteUser(String userId) {
        passwordDao.deleteById(userId);
        userDao.deleteById(userId);
    }

    /**
     * Change a password for a user when the current password is known.
     */
    public boolean changePassword(PasswordChangeRequest passwordChangeRequest, User user) {
        // First validate that the current password is valid
        Optional<Password> p = passwordDao.getByUsername(user.email);
        if (p.isPresent()) {
            Password password = p.get();
            // Determine if the provided current password matches the hashed password in the database
            if (BCrypt.checkpw(passwordChangeRequest.currentPassword, password.password)) {
                password.password = Password.encryptPassword(passwordChangeRequest.newPassword);
                passwordDao.replaceById(password.id, password);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Send an email with the password reset link to a user that has forgotten their password
     */
    public Optional<PasswordResetRequest> generatePasswordReset(PasswordResetRequest passwordResetRequest) {
        log.info("Generating password reset request for {}", passwordResetRequest.email);

        Optional<Password> password = passwordDao.getByUsername(passwordResetRequest.email);
        if (password.isPresent()) {
            // A valid password entry was found, so generate a token, save the request and send
            // the email.
            PasswordResetRequest requestToStore = new PasswordResetRequest();
            requestToStore.id = UUID.randomUUID().toString();
            requestToStore.completionUrl = passwordResetRequest.completionUrl;
            requestToStore.email = passwordResetRequest.email;
            requestToStore.created = ZonedDateTime.now();

            // Store the request
            passwordResetRequestDao.save(requestToStore);

            // Send the email
            try {
                // Create the email from the FreeMarker template
                Template template = ApiServer.freeMarkerConfiguration.getTemplate("passwordReset.ftl");
                Writer stringOut = new StringWriter();
                Map<String, Object> model = new HashMap<>();
                model.put("email", requestToStore.email);
                model.put("resetLink", requestToStore.completionUrl.concat("?resetRequestId=").concat(requestToStore.id));
                template.process(model, stringOut);

                SendEmailRequest emailRequest = new SendEmailRequest();
                emailRequest.setDestination(new Destination().withToAddresses(requestToStore.email));
                emailRequest.setMessage(new Message().withSubject(new Content().withData("FlowMSP Password Reset")).withBody(new Body().withHtml(new Content().withData(stringOut.toString()))));
                emailRequest.setSource(systemEmail);

                ses.sendEmail(emailRequest);
            } catch (Exception e) {
                log.error("Error sending password reset email", e);
                //return Optional.empty();
            }

            // Return the request
            log.info("Successfully sent password reset email to {}", passwordResetRequest.email);
            passwordResetRequest.id = requestToStore.id;
            return Optional.of(passwordResetRequest);
        } else {
            log.error("Cannot generate password reset for non-existent email {}", passwordResetRequest.email);
            return Optional.empty();
        }
    }

    public boolean completePasswordReset(PasswordResetRequestCompletion prrc) {
        var email = prrc.email.toLowerCase();

        log.info("Completing password reset request for {}", prrc.email);

        Optional<PasswordResetRequest> prr = passwordResetRequestDao.getById(prrc.resetRequestId);
        if (prr.isPresent() && prr.get().attempted == null) {
            PasswordResetRequest originalRequest = prr.get();
            originalRequest.attempted = ZonedDateTime.now();
            passwordResetRequestDao.replaceById(originalRequest.id, originalRequest);

            if (email.equals(originalRequest.email)) {
                Optional<Password> pword = passwordDao.getByUsername(email);
                if (pword.isPresent()) {
                    Password password = pword.get();
                    password.password = Password.encryptPassword(prrc.newPassword);
                    passwordDao.replaceById(password.id, password);
                    return true;
                } else {
                    log.error("Can't find user with email {} for password reset", prrc.email);
                    return false;
                }
            } else {
                log.error("Password reset request mismatched email, original request {}, confirmation {}", prr.get().email, prrc.email);
                return false;
            }
        } else {
            log.error("Unable to find password reset request for {}, requestId {}", prrc.email, prrc.resetRequestId);
            return false;
        }
    }

    private void AppendInfo(StringBuilder sb, String msg) {
        sb.append("NOTE: " + msg + System.lineSeparator());
    }

    private void AppendError(StringBuilder sb, String msg) {
        sb.append("WARN: " + msg + System.lineSeparator());
    }

    private void AppendMessage(StringBuilder sb, String msg) {
        sb.append("INFO: " + msg + System.lineSeparator());
    }

    private void AppendSummary(StringBuilder sb, StringBuilder msg) {
        sb.append("[Summary]:@" + DateTime.now().toString("dd-MMM-yyyy HH:mm:ss") + System.lineSeparator() + msg + System.lineSeparator());
    }

    private String generateRandomPassword() {
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        int len = SALTCHARS.length();
        Random rnd = new Random();
        while (salt.length() < 8) { // length of the random string.
            int index = rnd.nextInt(len);
            if (index < 0) {
                index = 0;
            } else if (index >= len) {
                index = len - 1;
            }
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    private String NVLString(String data) {
        if (data == null) {
            return "";
        }
        return data;
    }

    public void uploadUsers(InputStream inputStream, Customer customer, String userId, UserUploadResponse returnResponse) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbSummary = new StringBuilder();
        int LineNumber = 1;

        try {
            AppendInfo(sb, "Process started");
            CsvParserSettings settings = new CsvParserSettings();

            //the file used in the example uses '\n' as the line separator sequence.
            //the line separator sequence is defined here to ensure systems such as MacOS and Windows
            //are able to process this file correctly (MacOS uses '\r'; and Windows uses '\r\n').
            settings.getFormat().setLineSeparator("\n");

            // creates a CSV parser
            CsvParser parser = new CsvParser(settings);
            parser.beginParsing(inputStream);

            String[] headerArr;
            headerArr = parser.parseNext();
            if (headerArr == null) {
                AppendError(sb, "File header is null");
                return;
            }

            String header = "";
            for (int ii = 0; ii < headerArr.length; ii++) {
                header = header + headerArr[ii].replace("﻿", "") + ",";
            }

            header = header.substring(0, header.length() - 1);
            char ch1 = header.charAt(0);
            if (!((ch1 >= 'a' && ch1 <= 'z') || (ch1 >= 'A' && ch1 <= 'Z'))) {
                header = header.substring(1);
            }

            ch1 = header.charAt(0);
            if (!((ch1 >= 'a' && ch1 <= 'z') || (ch1 >= 'A' && ch1 <= 'Z'))) {
                header = header.substring(1);
            }

            String headerConstant = "First Name,Last Name,Email Address,Role";
            if (!header.equalsIgnoreCase(headerConstant)) {
                AppendError(sb, "Header Incorrect, Found:" + header + ": Expecting:" + headerConstant);
                return;
            }

            String[] data;

            while ((data = parser.parseNext()) != null) {
                LineNumber++;

                if (data.length <= 0) {
                    AppendError(sb, "Skipping blank record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }
                Boolean allNull = true;
                //If All NULL then also continue
                for (int ii = 0; ii < data.length; ii++) {
                    if (data[ii] != null) {
                        allNull = false;
                        break;
                    }
                }
                if (allNull) {
                    AppendError(sb, "Skipping null record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }

                int pos = 0;
                String firstName = data[pos++];
                String lastName = data[pos++];
                String email = data[pos++].toLowerCase();
                String role = data[pos++].toUpperCase();

                if (NVLString(firstName).isEmpty()) {
                    AppendError(sb, "First name can't be blank. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }
                if (NVLString(lastName).isEmpty()) {
                    AppendError(sb, "Last name can't be blank. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }
                if (NVLString(email).isEmpty()) {
                    AppendError(sb, "Email can't be blank. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }
                if (NVLString(role).isEmpty()) {
                    AppendError(sb, "Role can't be blank. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }

                String password = generateRandomPassword();
                Optional<Password> existingUser = passwordDao.getByUsername(email);
                if (existingUser.isPresent()) {
                    AppendError(sb, "Duplicate user. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }
                // The role must be one of the acceptable values
                UserRole userRole;
                try {
                    userRole = UserRole.valueOf(role);
                } catch (Exception e) {
                    userRole = null;
                }
                if (userRole == null) {
                    AppendError(sb, "Invalid user role. Record line:" + LineNumber);
                    returnResponse.recordKountFail++;
                    continue;
                }

                User u = new User(UUID.randomUUID().toString(), email, firstName, lastName, userRole, customer);
                Password p = new Password(u, Password.encryptPassword(password), customer);

                userDao.save(u);
                passwordDao.save(p);
                sbSummary.append(email + ", " + password + System.lineSeparator());
                returnResponse.recordKountInsert++;
            }

            returnResponse.successFlag = 0;
            returnResponse.msg = "Upload process completed";

        } catch (Exception ex) {
            returnResponse.successFlag = 99;
            AppendError(sb, "Unhandled Exception causes error at record line:" + LineNumber + " Internal error:" + ex);

            returnResponse.msg = "Exception occured while processing:system message:" + ex;
        } finally {
            AppendInfo(sb, "Process completed.");
            returnResponse.recordKount = returnResponse.recordKountInsert + returnResponse.recordKountUpdate + returnResponse.recordKountFail;
            AppendInfo(sb, "Record(s) processed:" + returnResponse.recordKount);
            AppendInfo(sb, "Record(s) inserted:" + returnResponse.recordKountInsert);
            AppendInfo(sb, "Record(s) updated:" + returnResponse.recordKountUpdate);
            AppendInfo(sb, "Record(s) failed:" + returnResponse.recordKountFail);
            AppendSummary(sb, sbSummary);

            returnResponse.log = sb.toString();
        }
    }
}
