package com.user.returnsassistant.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class NoUtils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private NoUtils() {
    }

    public static String sessionNo() {
        return "S" + now() + random();
    }

    public static String afterSaleNo() {
        return "AS" + now() + random();
    }

    public static String ticketNo() {
        return "T" + now() + random();
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String random() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
    }
}
