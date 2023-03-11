package com.example.mqtt;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: badBoy
 * @create: 2023-03-11 14:13
 * @Description: TODO MQTT客户端处理发送数据
 */
@RestController
@RequestMapping("/mqtt")
public class MqttClientController {

    /**
     * 发送网关
     */
    @Autowired
    private MqttPublishGateway mqttPublishGateway;

    @Autowired
    @Qualifier("inbound")
    private MqttPahoMessageDrivenChannelAdapter messageProducer;


    /**
     * 测试：发布消息
     */
    @GetMapping("/testPublish")
    public String sentTest(@RequestParam("topic") String topic,
                           @RequestParam("payload") String payload) {
        mqttPublishGateway.sendToMqtt(topic, 1, payload);
        return "OK";
    }

    /**
     * 测试订阅信息
     */
    @GetMapping("/testSubscribe")
    @ServiceActivator(inputChannel = "MQTT_INPUT_CHANNEL")
    public String testSubscribe() throws MqttException {
        messageProducer.addTopic("TOPICS");
        return "OK";
    }


}
