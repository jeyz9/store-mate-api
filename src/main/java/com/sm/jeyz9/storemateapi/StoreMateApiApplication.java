package com.sm.jeyz9.storemateapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StoreMateApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreMateApiApplication.class, args);
    }

}
