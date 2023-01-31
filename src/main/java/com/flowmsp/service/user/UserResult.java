package com.flowmsp.service.user;

import com.flowmsp.domain.user.User;

public class UserResult {
    public String errorMessage;
    public User   user;

    public UserResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public UserResult(User user) {
        this.user = user;
    }
}
