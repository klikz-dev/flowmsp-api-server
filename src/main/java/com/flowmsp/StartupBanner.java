package com.flowmsp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StartupBanner {
    public static void printBannerFromResource(String resource) {
        InputStream is = ApiServer.class.getResourceAsStream(resource);
        if (is != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                br.lines().forEach(System.out::println);
            }
            catch (Exception e) {
                System.out.println(e.toString());
                // I guess it is okay to eat the error not letting it print the banner
            }
        }
    }

}
