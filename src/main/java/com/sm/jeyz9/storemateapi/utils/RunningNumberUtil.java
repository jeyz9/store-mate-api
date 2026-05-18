package com.sm.jeyz9.storemateapi.utils;

import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor
public class RunningNumberUtil {
    public static String generate(String prefix, Long id) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return String.format("%s-%s-%04d", prefix, date, id);
    }
}
