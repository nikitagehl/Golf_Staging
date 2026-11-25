package com.nikitagehlod.utils;

/** Builds full site URLs from the "URL" key in data.properties. */
public final class SiteUrls {

    private SiteUrls() {
    }

    /** Base URL without the trailing slash, e.g. https://devbeta.betspertsgolf.com */
    public static String base() {
        return propertyReader.readKey("URL").trim().replaceAll("/+$", "");
    }

    /** Full URL for a site path, e.g. of("/sign-in"). */
    public static String of(String path) {
        return base() + (path.startsWith("/") ? path : "/" + path);
    }

    /** Host name of the site, e.g. devbeta.betspertsgolf.com */
    public static String host() {
        return java.net.URI.create(base()).getHost();
    }
}
