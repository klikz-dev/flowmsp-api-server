package com.flowmsp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamConverter {

    static public byte[] isToBytes(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            org.apache.commons.io.IOUtils.copy(is, baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    static public InputStream bytesToIs(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

}
