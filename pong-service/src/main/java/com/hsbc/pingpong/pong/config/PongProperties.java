package com.hsbc.pingpong.pong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hsbc.pingpong")
public class PongProperties {

    private int maxRequestsPerSecond = 1;

}
