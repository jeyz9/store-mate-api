package com.sm.jeyz9.storemateapi.utils;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor
public class RunningNumberUtil {
    public static String generate(String prefix) {
        return String.format(
                "%s-%s-%04d",
                prefix,
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                ),
                ThreadLocalRandom.current().nextInt(1000, 9999)
        );
    }
}
