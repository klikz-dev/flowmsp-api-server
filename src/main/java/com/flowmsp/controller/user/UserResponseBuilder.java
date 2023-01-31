package com.flowmsp.controller.user;

import com.flowmsp.domain.user.User;
import com.flowmsp.service.BsonUtil;
import org.jooq.lambda.function.Function2;
import spark.Request;

public interface UserResponseBuilder {

    static UserResponse build(Request req, User user) {
        UserResponse userResponse = new UserResponse();
        if(user != null) {
            userResponse.customerRef = user.customerRef;
            userResponse.email = user.email;
            userResponse.firstName = user.firstName;
            userResponse.id = user.id;
            userResponse.lastName = user.lastName;
            userResponse.role = user.role;
            userResponse.uiConfig = BsonUtil.convertToObjectMap(user.uiConfig);
            user.href = href(req, user.customerRef.customerSlug, user.id);
            user.customerRef.href = customerHref(req, user.customerRef.customerSlug, user.customerRef.customerId);
            userResponse.isOnDuty = user.isOnDuty;
        }
        return userResponse;
    }

    Function2<Request, User, UserResponse> responseBuilder = UserResponseBuilder::build;

    static String href(Request req, String customerSlug, String userId) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());
        return String.format("%s/api/%s/user/%s", protocolHost, customerSlug, userId);
    }

    static String customerHref(Request req, String customerSlug, String customerId) {
        String protocol = req.raw().isSecure() ? "https://" : "http://";
        String protocolHost = protocol.concat(req.host());
        return String.format("%s/api/%s/customer/%s", protocolHost, customerSlug, customerId);
    }
}
