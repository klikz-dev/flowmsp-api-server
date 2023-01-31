package com.flowmsp.domain.fcmData;

public class FcmResponseResult {

    public String registrationToken;
    public String errorCode;

    public FcmResponseResult() {
    }

    public FcmResponseResult(String registrationToken) {
        this.registrationToken = registrationToken;
    }

    public FcmResponseResult(String registrationToken, String errorCode) {
        this.registrationToken = registrationToken;
        this.errorCode = errorCode;
    }
}
