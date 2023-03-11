package com.example.mqtt;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: badBoy
 * @create: 2023-03-11 13:53
 * @Description:
 */
@Data
public class MqttVO {

    @ApiModelProperty("订阅的主题")
    public String topic;

    @ApiModelProperty("发送的内容")
    public String payload;

}
