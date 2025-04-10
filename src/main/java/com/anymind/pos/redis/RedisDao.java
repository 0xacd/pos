package com.anymind.pos.redis;


import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Component
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisDao {
    @Resource
    private RedisTemplate redisTemplate;

    @Value("${redis.key.pre:''}")
    private String pre;

    public void save(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(pre + key, value, time, timeUnit);
    }

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(pre + key, value);
    }

    public boolean exist(String key) {
        return redisTemplate.hasKey(pre + key);
    }

    public void updateValueWithoutChangingTTL(String key, Object newValue) {
        ValueOperations valueOps = redisTemplate.opsForValue();
        Long expire = redisTemplate.getExpire(pre + key, TimeUnit.SECONDS);
        valueOps.set(pre + key, newValue);
        if (expire != null && expire > 0) {
            redisTemplate.expire(pre + key, expire, TimeUnit.SECONDS);
        }
    }


    public void saveHashSet(String key, HashSet<Object> hashSet) {
        Object[] array = hashSet.toArray();
        redisTemplate.opsForSet().add(pre + key, array);
    }

    public HashSet<Object> getHashSet(String key) {
        HashSet<Object> hashSet = new HashSet<>();
        hashSet.addAll(Objects.requireNonNull(redisTemplate.opsForSet().members(pre + key)));
        return hashSet;
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(pre + key);
    }

    public Long getChatServerSetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(pre + key);
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(pre + key, timeout, unit);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(pre + key);
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pre + pattern);
        if (!keys.isEmpty()) {
            for (String key : keys) {
                redisTemplate.delete(key);
            }
        }
    }

    public boolean acquireLock(String key, long timeout, TimeUnit timeUnit) {
        String lockKey = pre + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", timeout, timeUnit);
        return Boolean.TRUE.equals(success); // Ensure null-safe Boolean comparison
    }

    public void releaseLock(String key) {
        String lockKey = pre + key;
        redisTemplate.delete(lockKey);
    }


}
