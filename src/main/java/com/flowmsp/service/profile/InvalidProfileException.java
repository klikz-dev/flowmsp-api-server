package com.flowmsp.service.profile;

public class InvalidProfileException extends Exception {
    public InvalidProfileException() {
        super();
    }

    public InvalidProfileException(String msg) {
        super(msg);
    }
}
