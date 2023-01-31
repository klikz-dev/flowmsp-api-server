package com.flowmsp.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class JwtUtilTest {
    @Test
    public void test() {
        String salt = "29u8298alkjskld3u83jkslajlsdj+ksluklkjakjlsdj9928823";
        String testString = "Test String";
        Date testLdt = new Date();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "Password Reset");
        claims.put("stringVal", testString);
        claims.put("ldtVal", testLdt.getTime());

        String token = JwtUtil.generateWithClaims(claims, salt);
        Map<String, Object> newClaims = JwtUtil.validateClaims(token, salt);

        Assert.assertEquals(testString, newClaims.get("stringVal"));
        Assert.assertEquals(testLdt.getTime(), newClaims.get("ldtVal"));

        String newSalt = "1234567890123456789012345678901234567890";
        Assert.assertNull(JwtUtil.validateClaims(token, newSalt));
    }
}
