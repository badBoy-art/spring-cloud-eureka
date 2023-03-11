package com.example.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

/**
 * @author: badBoy
 * @create: 2023-03-11 13:54
 * @Description:
 */
@Slf4j
@Component
public class MqttReceiveHandler implements MessageHandler {

    @Override
    @ServiceActivator(inputChannel = "MQTT_INPUT_CHANNEL")
    public void handleMessage(Message<?> message) throws MessagingException {
        log.info("收到订阅消息: {}", message);
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
        log.info("消息主题：{}", topic);
    }

}
