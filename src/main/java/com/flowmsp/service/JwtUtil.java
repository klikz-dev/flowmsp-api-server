package com.flowmsp.service;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtGenerator;

import java.util.Map;

public interface JwtUtil {
    String JWT_SALT = "12345678901234567890123456789012";

    /**
     * Method that can be used by the HeaderClient to authenticate JWT Bearer tokens
     *
     * @param credentials
     * @param context
     * @throws HttpAction
     * @throws CredentialsException
     */
    static void validate(Credentials credentials, WebContext context) throws HttpAction, CredentialsException {
        String token = ((TokenCredentials) credentials).getToken();
        String[] tokenParts = token.split(" +");
        if(tokenParts.length == 2 && tokenParts[0].equals("Bearer")) {
            token = tokenParts[1];
            JwtAuthenticator authenticator = new JwtAuthenticator();
            authenticator.addSignatureConfiguration(new SecretSignatureConfiguration(JWT_SALT));

            if (CommonHelper.isNotBlank(token)) {
                CommonProfile profile = authenticator.validateToken(token);
                credentials.setUserProfile(profile);
            }
        }
    }


    static String generate(CommonProfile profile) {
        // Build the response with the token. This will allow for future inclusion of a
        // refresh token as well.
        JwtGenerator generator = new JwtGenerator(new SecretSignatureConfiguration(JWT_SALT));
        return generator.generate(profile);
    }

    static String generateWithClaims(Map<String, Object> claims, String salt) {
        JwtGenerator generator = new JwtGenerator(new SecretSignatureConfiguration(salt));
        return generator.generate(claims);
    }

    static Map<String, Object> validateClaims(String token, String salt) {
        JwtAuthenticator authenticator = new JwtAuthenticator();
        authenticator.addSignatureConfiguration(new SecretSignatureConfiguration(salt));
        try {
            return authenticator.validateTokenAndGetClaims(token);
        }
        catch (Exception e) {
            return null;
        }
    }
}
