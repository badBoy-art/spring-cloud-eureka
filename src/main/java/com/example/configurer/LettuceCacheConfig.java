package com.example.configurer;

import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author xuedui.zhao
 * @create 2019-07-09
 */
@Configuration
@EnableCaching
@AutoConfigureBefore(RedisAutoConfiguration.class)
public class LettuceCacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // RedisStandaloneConfiguration这个配置类是Spring Data Redis2.0后才有的~~~
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        // 2.0后的写法
        configuration.setHostName("127.0.0.1");
        //configuration.setPassword(RedisPassword.of("123456"));
        configuration.setPort(6379);
        configuration.setDatabase(0);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration);
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
    @Override
    public CacheManager cacheManager() {
        // 1.x是这么配置的：仅供参考
        //RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
        //cacheManager.setDefaultExpiration(ONE_HOUR * HOURS_IN_ONE_DAY);
        //cacheManager.setUsePrefix(true);

        // --------------2.x的配置方式--------------
        // 方式一：直接create
        //RedisCacheManager redisCacheManager = RedisCacheManager.create(redisConnectionFactory());
        // 方式二：builder方式（推荐）

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1)) //Duration.ZERO表示永不过期（此值一般建议必须设置）
                //.disableKeyPrefix() // 禁用key的前缀
                //.disableCachingNullValues() //禁止缓存null值

                //=== 前缀我个人觉得是非常重要的，建议约定：注解缓存一个统一前缀、RedisTemplate直接操作的缓存一个统一前缀===
                //.prefixKeysWith("baidu:") // 底层其实调用的还是computePrefixWith() 方法，只是它的前缀是固定的（默认前缀是cacheName，此方法是把它固定住，一般不建议使用固定的）
                //.computePrefixWith(CacheKeyPrefix.simple()); // 使用内置的实现
                .computePrefixWith(cacheName -> "caching:" + cacheName) // 自己实现，建议这么使用(cacheName也保留下来了)
                ;

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory())
                // .disableCreateOnMissingCache() // 关闭动态创建Cache
                //.initialCacheNames() // 初始化时候就放进去的cacheNames（若关闭了动态创建，这个就是必须的）
                .cacheDefaults(configuration) // 默认配置（强烈建议配置上）。  比如动态创建出来的都会走此默认配置
                //.withInitialCacheConfigurations() // 个性化配置  可以提供一个Map，针对每个Cache都进行个性化的配置（否则是默认配置）
                //.transactionAware() // 支持事务
                .build();
        return redisCacheManager;
    }

}
