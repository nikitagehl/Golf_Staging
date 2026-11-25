package com.nikitagehlod.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/** Creates unique sign-up data, so every run uses a brand new username and email. */
public final class TestDataGenerator {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MMddHHmmss");

    private TestDataGenerator() {
    }

    private static String suffix() {
        return LocalDateTime.now().format(STAMP) + ThreadLocalRandom.current().nextInt(100, 999);
    }

    /** e.g. qaauto1125114512345 (letters + digits only, no spaces). */
    public static String uniqueUsername() {
        return "qaauto" + suffix();
    }

    /** e.g. qaauto1125114512345@test.com - domain comes from signup.emailDomain in data.properties. */
    public static String uniqueEmail() {
        String domain = propertyReader.readKey("signup.emailDomain");
        if (domain == null || domain.isBlank()) {
            domain = "test.com";
        }
        return "qaauto" + suffix() + "@" + domain.trim();
    }
}
