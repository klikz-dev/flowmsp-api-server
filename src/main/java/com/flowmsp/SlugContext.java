package com.flowmsp;

import java.util.Optional;

public class SlugContext {

    private static ThreadLocal<String> slug = new ThreadLocal<>();
    private static ThreadLocal<String> partnerSlug = new ThreadLocal<>();

    public static void clearSlug() {
        slug.remove();
    }

    public static void setSlug(String newSlug) {
        slug.set(newSlug);
    }

    public static Optional<String> getSlug() {
        return Optional.ofNullable(slug.get());
    }
    
    public static void clearPartnerSlug() {
    	partnerSlug.remove();
    }

    public static void setPartnerSlug(String newSlug) {
    	partnerSlug.set(newSlug);
    }

    public static Optional<String> getpartnerSlug() {
        return Optional.ofNullable(partnerSlug.get());
    }

}
