package com.example.configurer;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author xuedui.zhao
 * @create 2019-07-09
 */
@Configuration
@EnableCaching
public class JedisCacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        // 2.0后的写法
        configuration.setHostName("127.0.0.1");
        //configuration.setPassword(RedisPassword.of("123456"));
        configuration.setPort(6379);
        configuration.setDatabase(0);


        JedisConnectionFactory factory = new JedisConnectionFactory(configuration);
        // Spring Data Redis1.x这么来设置  2.0后建议使用RedisStandaloneConfiguration来取代
        //factory.setHostName("10.102.132.150");
        //factory.setPassword("123456");
        //factory.setPort(6379);
        //factory.setDatabase(0);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }

}
