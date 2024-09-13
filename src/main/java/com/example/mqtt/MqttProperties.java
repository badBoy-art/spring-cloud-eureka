package com.example.mqtt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: badBoy
 * @create: 2023-03-11 13:51
 * @Description:
 */
@Data
@Component
@ConfigurationProperties(prefix = "mqtt.config")
public class MqttProperties {

    private String username;

    private String password;

    private String hostUrl;

    private String clientId;

    private String defaultTopic;

    private int timeout;

    private int keepalive;

    private boolean enabled;

}
