package com.flowmsp.controller.auth;

import com.flowmsp.service.JwtUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

import java.util.Optional;

public class AuthTokenController {
    public AuthTokenResponse generateToken(Request req, Response res) {
        SparkWebContext context                 = new SparkWebContext(req, res);
        ProfileManager<CommonProfile> manager   = new ProfileManager<>(context);
        Optional<CommonProfile> profile         = manager.get(false);

        if(profile.isPresent()) {
            CommonProfile commonProfile = profile.get();
            String token = JwtUtil.generate(commonProfile);
            AuthTokenResponse resp = new AuthTokenResponse(token);
            Object msg = commonProfile.getAttribute("msg");
            String msgStr = "";
            if (msg != null) {
            	msgStr = msg.toString();
            }
            resp.buildLinks(req,
                            (String)commonProfile.getAttribute("customerId"),
                            (String)commonProfile.getAttribute("slug"),
                            commonProfile.getId(),
                            msgStr);
            return resp;
        }
        else {
            res.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return new AuthTokenResponse();
        }
    }
}
