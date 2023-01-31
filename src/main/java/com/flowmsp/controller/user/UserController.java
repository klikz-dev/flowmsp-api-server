package com.flowmsp.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.SlugContext;
import com.flowmsp.controller.StatusResponse;
import com.flowmsp.db.*;
import com.flowmsp.domain.auth.RegistrationLink;
import com.flowmsp.domain.customer.Customer;
import com.flowmsp.domain.fcmData.FcmData;
import com.flowmsp.domain.user.PasswordResetRequest;
import com.flowmsp.domain.user.User;
import com.flowmsp.service.BsonUtil;
import com.flowmsp.service.debugpanel.debugPanel;
import com.flowmsp.service.patch.PatchUtil;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.flowmsp.service.user.*;
import com.google.common.base.Strings;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bson.BsonValue;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Updates.set;

public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final CustomerDao customerDao;
    private final UserDao userDao;
    private final UserService userService;
    private final ObjectMapper mapper;
    private final DebugInfoDao debugInfoDao;
    private final FcmDataDao fcmDataDao;
    private final RegistrationLinkDao registrationLinkDao;

    public UserController(CustomerDao customerDao, UserDao userDao, DebugInfoDao debugInfoDao, FcmDataDao fcmDataDao, RegistrationLinkDao registrationLinkDao, UserService userService, ObjectMapper mapper) {
        this.customerDao = customerDao;
        this.userDao = userDao;
        this.debugInfoDao = debugInfoDao;
        this.fcmDataDao = fcmDataDao;
        this.registrationLinkDao = registrationLinkDao;
        this.userService = userService;
        this.mapper = mapper;
    }

    public UserResponse get(Request req, Response res) {
        String id = req.params("id");
        Optional<User> u = userDao.getById(id);

        return u.map(UserResponseBuilder.responseBuilder.applyPartially(req))
                .orElseGet(() -> {
                    res.status(404);
                    return null;
                });
    }

    /*
     * Gets all the entities
     */
    public List<UserResponse> getAll(Request req, Response res) {
        // The users need to be processed before returning to add the hrefs and to properly format the uiConfig values
        return userDao.getAll()
                .stream()
                .map(UserResponseBuilder.responseBuilder.applyPartially(req))
                .collect(Collectors.toList());
    }

    public UserResponse addUser(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        try {
            xray.SetCustomerInfo(req, res, "ADD_USER");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            UserRequest userRequest = mapper.readValue(req.body(), UserRequest.class);
            UserResult userResult = userService.addUser(userRequest, customer);
            if (userResult.user == null) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            } else {
                return UserResponseBuilder.build(req, userResult.user);
            }
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        } finally {
            xray.commitLog();
        }
    }

    public UserResponse addUserMainData(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        try {
            xray.SetCustomerInfo(req, res, "ADD_USER_MAIN_DATA");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            UserMainDataRequest userRequest = mapper.readValue(req.body(), UserMainDataRequest.class);

            UserResult userResult = userService.addUserMainData(userRequest, customer);

            RegistrationLink registrationLink = null;
            if (userResult.user != null) {
                Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByUsername(userRequest.email);
                if (registrationLinkOpt.isPresent()) {
                    registrationLink = registrationLinkOpt.get();
                    userService.sendRegistrationLink(registrationLink, getRegistrationLinkFull(req, registrationLink));
                }
            }

            if (userResult.user == null) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            } else {
                UserResponse userResponse = UserResponseBuilder.build(req, userResult.user);
                if(registrationLink != null)
                userResponse.registrationLink = getRegistrationLinkFull(req, registrationLink);
                return userResponse;
            }
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        } finally {
            xray.commitLog();
        }
    }

    private String getRegistrationLinkFull(Request req, RegistrationLink registrationLink){
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());
        String registerUrlStart = String.format("%s/signUpForm", protocolHost);

        return registerUrlStart.concat("?linkPart=").concat(registrationLink.linkPart);
    }

    public UserResponse addUserSecondaryData(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        try {
            String linkPart = req.params("linkPart");

            xray.SetCustomerInfo(req, res, "ADD_USER_SECONDARY_DATA");

            boolean isValid = userService.validateRegistrationLink(linkPart);

            if (!isValid) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }

            Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByLinkPart(linkPart);
            if (registrationLinkOpt.isEmpty()) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }
            RegistrationLink registrationLink = registrationLinkOpt.get();

            String customerId = registrationLink.customerId;
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            SlugContext.setSlug(customer.slug);

            UserSecondaryDataRequest userRequest = mapper.readValue(req.body(), UserSecondaryDataRequest.class);
            UserResult userResult = userService.addUserSecondaryData(userRequest, linkPart);
            if (userResult.user == null) {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            } else {
                return UserResponseBuilder.build(req, userResult.user);
            }
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        } finally {
            xray.commitLog();
        }
    }

    public StatusResponse validateRegistrationLink(Request req, Response res) {
        try {
            String linkPart = req.params("linkPart");

            boolean isValid = userService.validateRegistrationLink(linkPart);

            StatusResponse statusResponse = new StatusResponse();
            statusResponse.success = isValid;
            return statusResponse;
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        }
    }

    public UserResponse getUserByRegistrationLink(Request req, Response res) {
        try {
            String linkPart = req.params("linkPart");

            log.info("getUserByRegistrationLink: " + linkPart);
            Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByLinkPart(linkPart);
            if (registrationLinkOpt.isEmpty()) {
                log.error("getUserByRegistrationLink: lookup by linkPart failed");
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }
            RegistrationLink registrationLink = registrationLinkOpt.get();

            String customerId = registrationLink.customerId;
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            log.info("getUserByRegistrationLink: slug=" + customer.slug);
            SlugContext.setSlug(customer.slug);

            Optional<User> userOpt = userDao.getById(registrationLink.userId);
            if (userOpt.isEmpty()) {
                log.error("getUserByRegistrationLink: lookup by userId failed: userID=" + registrationLink.userId);
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }

            User user = userOpt.get();
            SlugContext.clearSlug();
            return UserResponseBuilder.build(req, user);
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        }
    }

    public StatusResponse resendRegistrationLink(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));

            ResendRegistrationLinkRequest userRequest = mapper.readValue(req.body(), ResendRegistrationLinkRequest.class);

            Optional<RegistrationLink> registrationLinkOpt = registrationLinkDao.getByUsername(userRequest.email);
            if (registrationLinkOpt.isEmpty()) {
                log.error("Link is missing");
                return null;
            }
            RegistrationLink registrationLink = registrationLinkOpt.get();
            userService.sendRegistrationLink(registrationLink, getRegistrationLinkFull(req, registrationLink));

            StatusResponse statusResponse = new StatusResponse();
            statusResponse.success = true;
            return statusResponse;
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error creating user", e);
            return null;
        }
    }

    public Object changePassword(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        try {
            xray.SetCustomerInfo(req, res, "CHANGE_PASSWORD");
            String id = req.params("id");
            Optional<User> u = userDao.getById(id);
            PasswordChangeRequest pcr = mapper.readValue(req.body(), PasswordChangeRequest.class);
            if (u.isPresent() && pcr != null && !Strings.isNullOrEmpty(pcr.currentPassword) && !Strings.isNullOrEmpty(pcr.newPassword)) {
                if (userService.changePassword(pcr, u.get())) {
                    res.status(HttpStatus.NO_CONTENT_204);
                    return null;
                } else {
                    res.status(HttpStatus.BAD_REQUEST_400);
                    return null;
                }
            } else {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }
        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("Error creating password change request", e);
            return null;
        } finally {
            xray.commitLog();
        }
    }

    public Map<String, Object> addRegistrationToken(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String userId = profile.getUserId();

            RegistrationTokenRequest rir = mapper.readValue(req.body(), RegistrationTokenRequest.class);

            if (rir != null && !Strings.isNullOrEmpty(rir.platform) && !Strings.isNullOrEmpty(rir.registrationToken)) {
                String platform = rir.platform;
                String registrationToken = rir.registrationToken;

                Optional<FcmData> fcmD = fcmDataDao.getByFieldValue("registrationToken", registrationToken);

                if (fcmD.isPresent()) {
                    FcmData fcmData = fcmD.get();
                    fcmData.customerId = customerId;
                    fcmData.userId = userId;
                    fcmData.modifiedOn = new Date();
                    fcmDataDao.replaceById(fcmData.id, fcmData);
                } else {
                    FcmData fcmData = new FcmData(UUID.randomUUID().toString(), customerId, userId, registrationToken, platform);
                    fcmData.createdOn = new Date();
                    fcmDataDao.save(fcmData);
                }

                res.status(HttpStatus.NO_CONTENT_204);
                return null;
            } else {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }

        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("Error creating password change request", e);
            return null;
        }
    }

    public Map<String, Object> deleteRegistrationToken(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String userId = profile.getUserId();

            RegistrationTokenRequest rir = mapper.readValue(req.body(), RegistrationTokenRequest.class);

            if (rir != null && !Strings.isNullOrEmpty(rir.registrationToken)) {
                String registrationToken = rir.registrationToken;
                fcmDataDao.deleteAllByFieldValue("registrationToken", registrationToken);
                res.status(HttpStatus.NO_CONTENT_204);
                return null;
            } else {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }

        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("Error creating password change request", e);
            return null;
        }
    }

    public Map<String, Object> setUIConfig(Request req, Response res) {
        String id = req.params("id");
        Optional<User> u = userDao.getById(id);

        if (u.isPresent()) {
            User user = u.get();
            try {
                Map<String, Object> d = mapper.readValue(req.body(), new TypeReference<Map<String, Object>>() {
                });
                Map<String, BsonValue> d2 = BsonUtil.convertToBsonValueMap(d);
                Optional<User> updatedUser = userDao.addUIConfig(user.id, d2);
                if (updatedUser.isPresent()) {
                    Map<String, BsonValue> config = updatedUser.get().uiConfig;
                    return BsonUtil.convertToObjectMap(config);
                } else {
                    res.status(500);
                    return new HashMap<>();
                }
            } catch (IOException e) {
                res.status(500);
                return new HashMap<>();
            }
        } else {
            res.status(404);
            return new HashMap<>();
        }
    }

    public Map<String, Object> getUIConfig(Request req, Response res) {
        String id = req.params("id");
        Optional<User> u = userDao.getById(id);
        if (u.isPresent()) {
            Map<String, BsonValue> config = u.get().uiConfig;
            Map<String, Object> config2 = BsonUtil.convertToObjectMap(config);
            return config2;
        } else {
            res.status(404);
            return new HashMap<>();
        }
    }

    public UserResponse updateUser(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        xray.SetCustomerInfo(req, res, "UPDATE_USER");
        String id = req.params("id");
        Optional<User> u = userDao.getById(id);
        if (u.isPresent()) {
            User user = u.get();
            try {
                User patchedUser = PatchUtil.patch(req.body(), user, User.class);
                userDao.replaceById(patchedUser.id, patchedUser);
                UserResponse userToReturn = UserResponseBuilder.build(req, patchedUser);
                res.header("Location", userToReturn.href);
                return userToReturn;
            } catch (Exception e) {
                log.error("Error updated user", e);
                res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
                return null;
            } finally {
                xray.commitLog();
            }
        } else {
            xray.commitLog();
            res.status(HttpStatus.NOT_FOUND_404);
            return null;
        }
    }

    public Object deleteUser(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        try {
            xray.SetCustomerInfo(req, res, "DELETE_USER");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);

            // Only users with the ADMIN role are allowed to delete users
            if (profile.getCommonProfile().getRoles().contains("ADMIN")) {
                String userId = profile.getCommonProfile().getId();
                String idToDelete = req.params("id");

                // You cannot delete yourself
                if (userId.equalsIgnoreCase(idToDelete)) {
                    res.status(HttpStatus.BAD_REQUEST_400);
                    return null;
                } else {
                    userService.deleteUser(idToDelete);
                    return null;
                }
            } else {
                res.status(HttpStatus.UNAUTHORIZED_401);
                return null;
            }
        } catch (Exception e) {
            log.error("Error deleting user", e);
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return null;
        } finally {
            xray.commitLog();
        }
    }

    public PasswordResetRequest passwordResetRequest(Request req, Response res) {
        try {
            PasswordResetRequest prr = mapper.readValue(req.body(), PasswordResetRequest.class);

            String protocol = req.raw().isSecure() ? "https://" : "http://";
            String protocolHost = protocol.concat(req.host());

            if (prr != null) {
                prr.completionUrl = String.format("%s/passwordResetForm", protocolHost);
                return userService.generatePasswordReset(prr)
                        .orElseGet(() -> {
                            res.status(HttpStatus.UNAUTHORIZED_401);
                            return null;
                        });
            } else {
                log.error("Unable to parse body of password reset request: {}", req.body());
                res.status(HttpStatus.BAD_REQUEST_400);
                return null;
            }
        } catch (Exception e) {
            log.error("Error processing password reset request", e);
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return null;
        }
    }

    public Object completePasswordReset(Request req, Response res) {
        try {
            log.info("Completing password reset: {} with parameters {}", req.body(), req.queryParams());
            PasswordResetRequestCompletion prrc = mapper.readValue(req.body(), PasswordResetRequestCompletion.class);

            if (prrc != null) {
                if (userService.completePasswordReset(prrc)) {
                    return null;
                } else {
                    res.status(HttpStatus.BAD_REQUEST_400);
                    return null;
                }
            } else {
                res.status(HttpStatus.BAD_REQUEST_400);
                return null;
            }
        } catch (Exception e) {
            log.error("Error processing password reset request", e);
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return null;
        }
    }

    public UserUploadResponse upload(Request req, Response res) {
        debugPanel xray = new debugPanel(debugInfoDao);
        UserUploadResponse returnResponse = new UserUploadResponse();
        try {
            xray.SetCustomerInfo(req, res, "USER_UPLOAD");
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            Customer customer = customerDao.getById(customerId).orElseThrow(() -> new RuntimeException("Cannot find customer object"));
            String userId = profile.getUserId();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload();

            FileItemIterator iter = upload.getItemIterator(req.raw());
            if (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream inputStream = item.openStream();
                userService.uploadUsers(inputStream, customer, userId, returnResponse);
            }
            return returnResponse;
        } catch (Exception e) {
            log.error("Error uploading hydrant data file", e);
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            returnResponse.successFlag = 1;
            returnResponse.msg = "Exception occured with message:" + e.toString();
            return returnResponse;
        } finally {
            xray.commitLog();
        }
    }

    public Map<String, Object> enableDuty(Request req, Response res) {
        return setDuty(req, res, true);
    }

    public Map<String, Object> disableDuty(Request req, Response res) {
        return setDuty(req, res, false);
    }

    public Map<String, Object> setDuty(Request req, Response res, boolean enable) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String userId = profile.getUserId();
            String slug = profile.getCustomerSlug();

            Boolean slugAlreadyPresent = true;

            Optional<String> slugAlready = SlugContext.getSlug();
            if (!slugAlready.isPresent()) {
                SlugContext.setSlug(slug);
                slugAlreadyPresent = false;
            }

            Optional<User> u = userDao.getById(userId);

            if (u.isPresent()) {
                userDao.updateById(u.get().id, set("isOnDuty", enable));
                res.status(HttpStatus.NO_CONTENT_204);
            } else {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
            }
            if (!slugAlreadyPresent) {
                SlugContext.clearSlug();
            }

            return null;
        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("Error creating password change request", e);
            return null;
        }
    }

}
