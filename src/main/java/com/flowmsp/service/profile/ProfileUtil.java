package com.flowmsp.service.profile;

import com.flowmsp.service.profile.InvalidProfileException;
import com.flowmsp.service.profile.MissingProfileException;
import com.google.common.base.Strings;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.sparkjava.SparkWebContext;
import spark.Request;
import spark.Response;

public class ProfileUtil {
    public static CommonProfile getProfile(Request req, Response res) throws MissingProfileException {
        SparkWebContext context               = new SparkWebContext(req, res);
        ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
        return manager.get(false).orElseThrow(MissingProfileException::new);
    }

    public static ValidatedProfile getValidatedProfile(Request req, Response res) throws MissingProfileException, InvalidProfileException {
        CommonProfile profile = getProfile(req, res);
        if(Strings.isNullOrEmpty((String)profile.getAttribute("customerId"))) {
            throw new InvalidProfileException("Profile missing customerId");
        }
        if(Strings.isNullOrEmpty((String)profile.getAttribute("slug"))) {
            throw new InvalidProfileException("Profile missing slug");
        }

        return new ValidatedProfile(profile);
    }
}
