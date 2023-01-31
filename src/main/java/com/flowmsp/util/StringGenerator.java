package com.flowmsp.util;

import java.util.Random;

public class StringGenerator {

    private static final String ALPHA_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String ALPHA_NUMERIC_SYMBOL_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()";

    public static String randomAlpha(int count) {
        return randomString(count, ALPHA_STRING);
    }

    public static String randomAlphaNumeric(int count) {
        return randomString(count, ALPHA_NUMERIC_STRING);
    }

    public static String randomAlphaNumericSymbol(int count) {
        return randomString(count, ALPHA_NUMERIC_SYMBOL_STRING);
    }

    private static String randomString(int count, String dataString){
        Random rd = new Random();
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * dataString.length());
            char ch = dataString.charAt(character);

            if(rd.nextBoolean())
                ch = Character.toLowerCase(ch);

            builder.append(ch);
        }
        return builder.toString();
    }

}
