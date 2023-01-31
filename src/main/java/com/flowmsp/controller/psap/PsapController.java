package com.flowmsp.controller.psap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowmsp.db.CustomerDao;
import com.flowmsp.db.DebugInfoDao;
import com.flowmsp.db.UserDao;
import com.flowmsp.service.profile.ProfileUtil;
import com.flowmsp.service.profile.ValidatedProfile;
import com.flowmsp.service.psap.PSAPService;
import com.flowmsp.service.psap.UnitFilterRequest;
import com.flowmsp.service.user.UserService;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.Map;

public class PsapController {
    private static final Logger log = LoggerFactory.getLogger(PsapController.class);

    private final CustomerDao customerDao;
    private final UserDao userDao;
    private final UserService userService;
    private final PSAPService psapService;
    private final ObjectMapper mapper;
    private final DebugInfoDao debugInfoDao;

    public PsapController(CustomerDao customerDao, UserDao userDao, DebugInfoDao debugInfoDao, UserService userService, PSAPService psapService, ObjectMapper mapper) {
        this.customerDao = customerDao;
        this.userDao = userDao;
        this.debugInfoDao = debugInfoDao;
        this.userService = userService;
        this.psapService = psapService;
        this.mapper = mapper;
    }

    public Map<String, Object> setUserUnits(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String customerId = profile.getCustomerId();
            String userId = profile.getUserId();

            UnitFilterRequest request = mapper.readValue(req.body(), UnitFilterRequest.class);
            if (request != null) {
                String registrationToken = request.registrationToken;
                List<String> unitIds = request.unitIds;
                psapService.saveDispatchFilter(registrationToken, unitIds);
                res.status(HttpStatus.NO_CONTENT_204);
                return null;
            } else {
                res.status(HttpStatus.UNPROCESSABLE_ENTITY_422);
                return null;
            }
        } catch (Exception e) {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            log.error("Error", e);
            return null;
        }
    }

    public PsapUnitCustomerResponse getUserUnits(Request req, Response res) {
        try {
            ValidatedProfile profile = ProfileUtil.getValidatedProfile(req, res);
            String userId = profile.getUserId();
            String customerId = profile.getCustomerId();
            String registrationToken = "";
            try { registrationToken = req.queryParams("registrationToken"); } catch (Exception ignored) {}
            List<PsapUnitCustomerModel> psapUnitCustomerResponses = psapService.getUnitsByToken(customerId, registrationToken);
            PsapUnitCustomerResponse response = PsapUnitCustomerResponse.build(req, psapUnitCustomerResponses);
           // res.status(org.eclipse.jetty.http.HttpStatus.OK_200);
            return response;
        } catch (Exception e) {
            res.status(org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR);
            log.error("Error", e);
            return new PsapUnitCustomerResponse();
        }
    }

}
