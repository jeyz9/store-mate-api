package com.sm.jeyz9.storemateapi.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class LineConfig {

    @Value("${line.channel.secret}")
    private String channelSecret;

    @Value("${line.channel.token}")
    private String channelToken;
}