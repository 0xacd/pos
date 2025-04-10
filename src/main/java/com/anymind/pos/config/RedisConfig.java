package com.anymind.pos.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
@Configuration
public class RedisConfig {

    public static String API_CACHE_PRE;

    @Value("${redis.key.pre:''}")
    private String pre;

    public final static String MIN_1 = "1min";
    public final static String SECOND_30 = "30s";
    public final static String SECOND_10 = "10s";
    public final static String SECOND_5 = "5s";
    public final static String SECOND_3 = "3s";
    public final static String MIN_3 = "3min";
    public final static String MIN_5 = "5min";
    public final static String MIN_10 = "10min";
    public final static String MIN_15 = "15min";
    public final static String MIN_30 = "30min";
    public final static String MIN_60 = "60min";
    public final static String DAY_1 = "1day";



    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        redisCacheConfiguration.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                .cacheDefaults(redisCacheConfiguration)
                .withInitialCacheConfigurations(getRedisCacheConfigurationMap())
                .transactionAware()
                .build();
        return redisCacheManager;
    }

    private RedisCacheConfiguration getRedisCacheConfiguration(int seconds) {
        API_CACHE_PRE = pre + "::" + "api::";
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .entryTtl(Duration.ofSeconds(seconds))
                .prefixCacheNameWith(API_CACHE_PRE);
        return redisCacheConfiguration;
    }

    private Map<String, RedisCacheConfiguration> getRedisCacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        redisCacheConfigurationMap.put(SECOND_30, this.getRedisCacheConfiguration(30));
        redisCacheConfigurationMap.put(SECOND_10, this.getRedisCacheConfiguration(10));
        redisCacheConfigurationMap.put(SECOND_5, this.getRedisCacheConfiguration(5));
        redisCacheConfigurationMap.put(SECOND_3, this.getRedisCacheConfiguration(3));
        redisCacheConfigurationMap.put(MIN_1, this.getRedisCacheConfiguration(60));
        redisCacheConfigurationMap.put(MIN_3, this.getRedisCacheConfiguration(3 * 60));
        redisCacheConfigurationMap.put(MIN_5, this.getRedisCacheConfiguration(5 * 60));
        redisCacheConfigurationMap.put(MIN_10, this.getRedisCacheConfiguration(10 * 60));
        redisCacheConfigurationMap.put(MIN_15, this.getRedisCacheConfiguration(15 * 60));
        redisCacheConfigurationMap.put(MIN_30, this.getRedisCacheConfiguration(30 * 60));
        redisCacheConfigurationMap.put(MIN_60, this.getRedisCacheConfiguration(60 * 60));

        redisCacheConfigurationMap.put(DAY_1, this.getRedisCacheConfiguration(24 * 60 * 60));
        return redisCacheConfigurationMap;
    }
}