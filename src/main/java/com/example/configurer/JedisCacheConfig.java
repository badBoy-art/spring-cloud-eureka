package com.example.configurer;

import com.example.service.impl.MessageDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author xuedui.zhao
 * @create 2019-07-09
 */
//@Configuration
//@EnableCaching
public class JedisCacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        // 2.0后的写法
        configuration.setHostName("127.0.0.1");
        //configuration.setPassword(RedisPassword.of("123456"));
        configuration.setPort(6379);
        //configuration.setDatabase(0);
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration);
        // Spring Data Redis1.x这么来设置  2.0后建议使用RedisStandaloneConfiguration来取代
        //factory.setHostName("10.102.132.150");
        //factory.setPassword("123456");
        //factory.setPort(6379);
        //factory.setDatabase(0);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new StringRedisSerializer());
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//        redisTemplate.setStringSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(executor());

        Map<MessageListenerAdapter, Collection<? extends Topic>> map = new HashMap<>();
        List<ChannelTopic> channelTopics = new ArrayList<>();
        ChannelTopic channelTopic = new ChannelTopic("chat");
        channelTopics.add(channelTopic);
        map.put(messageListenerAdapter, channelTopics);
        container.setMessageListeners(map);

        return container;
    }

    @Bean
    public TaskExecutor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }

    @Bean
    public MessageDelegate messageDelegate() {
        return new MessageDelegate();
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(MessageDelegate messageDelegate) {
        // handleMessage 参数消息来的时候要调用的方法 默认是 handleMessage
        return new MessageListenerAdapter(messageDelegate, "handleMessage");
    }

}
