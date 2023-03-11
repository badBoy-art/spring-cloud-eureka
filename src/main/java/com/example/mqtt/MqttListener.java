package com.example.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.stereotype.Component;

/**
 * @author: badBoy
 * @create: 2023-03-11 14:12
 * @Description:
 */
@Slf4j
@Component
public class MqttListener {

    /**
     * 连接失败的事件通知
     *
     * @param mqttConnectionFailedEvent
     */
    @EventListener(classes = MqttConnectionFailedEvent.class)
    public void listenerAction(MqttConnectionFailedEvent mqttConnectionFailedEvent) {
        log.info("连接失败的事件通知");
    }

    /**
     * 已发送的事件通知
     *
     * @param mqttMessageSentEvent
     */
    @EventListener(classes = MqttMessageSentEvent.class)
    public void listenerAction(MqttMessageSentEvent mqttMessageSentEvent) {
        log.info("已发送的事件通知[{}]", mqttMessageSentEvent.toString());
    }

    /**
     * 已传输完成的事件通知
     * 1.QOS == 0,发送消息后会即可进行此事件回调，因为不需要等待回执
     * 2.QOS == 1，发送消息后会等待ACK回执，ACK回执后会进行此事件通知
     * 3.QOS == 2，发送消息后会等待PubRECV回执，知道收到PubCOMP后会进行此事件通知
     *
     * @param mqttMessageDeliveredEvent
     */
    @EventListener(classes = MqttMessageDeliveredEvent.class)
    public void listenerAction(MqttMessageDeliveredEvent mqttMessageDeliveredEvent) {
        log.info("已传输完成的事件通知");
    }

    /**
     * 消息订阅的事件通知
     *
     * @param mqttSubscribedEvent
     */
    @EventListener(classes = MqttSubscribedEvent.class)
    public void listenerAction(MqttSubscribedEvent mqttSubscribedEvent) {
        log.info("消息订阅的事件通知");
        log.info("成功订阅到主题: info={}", mqttSubscribedEvent.toString());
    }

}
