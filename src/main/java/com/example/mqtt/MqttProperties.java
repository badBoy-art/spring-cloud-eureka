package com.example.mqtt;

import io.swagger.annotations.ApiModelProperty;
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

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("地址")
    private String hostUrl;

    @ApiModelProperty("客户端id")
    private String clientId;

    @ApiModelProperty("订阅主题")
    private String defaultTopic;

    @ApiModelProperty("超时时间")
    private int timeout;

    @ApiModelProperty("心跳")
    private int keepalive;

    @ApiModelProperty("mqtt开关")
    private boolean enabled;

}
