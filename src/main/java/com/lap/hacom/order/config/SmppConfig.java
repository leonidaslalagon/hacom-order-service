package com.lap.hacom.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "smpp")
public class SmppConfig {

    private String host;
    private int port;
    private String systemId;
    private String password;
    private String systemType;
    private byte interfaceVersion;
    private byte addressTon;
    private byte addressNpi;
    private String sourceAddress;
    private boolean enabled;

}
