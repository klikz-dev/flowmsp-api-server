package com.flowmsp.service.profile;

import org.pac4j.core.profile.CommonProfile;

public class ValidatedProfile {
    private final CommonProfile commonProfile;

    ValidatedProfile(CommonProfile commonProfile) {
        this.commonProfile = commonProfile;
    }

    public CommonProfile getCommonProfile() {
        return commonProfile;
    }

    public String getCustomerId() {
        return (String)commonProfile.getAttribute("customerId");
    }

    public String getCustomerSlug() {
        return (String)commonProfile.getAttribute("slug");
    }
    
    public String getUserId() {
    	return commonProfile.getId();
    }
    
    public String getUserName() {
    	return commonProfile.getUsername();
    }
}
