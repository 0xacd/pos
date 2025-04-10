package com.anymind.pos.infra;

import com.anymind.pos.config.TestRedisConfiguration;
import com.anymind.pos.redis.RedisDao;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestRedisConfiguration.class)
@ActiveProfiles("junit")
@Slf4j
public class RedisDaoIntegrationTest {

    @Resource
    private RedisDao redisDao;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_KEY = "testKey";
    private static final String TEST_SET_KEY = "testSetKey";
    private static final String LOCK_KEY = "lockKey";

    @BeforeEach
    public void setUp() {
        // Ensure a clean Redis state before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @AfterEach
    public void tearDown() {
        // Clean up after each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    public void testSaveWithTTL() {
        // Arrange
        String value = "testValue";
        long timeout = 5L;

        // Act
        redisDao.save(TEST_KEY, value, timeout, TimeUnit.SECONDS);
        Object result = redisDao.get(TEST_KEY);
        Long ttl = redisTemplate.getExpire(TEST_KEY, TimeUnit.SECONDS);

        // Assert
        assertEquals(value, result);
        assertTrue(ttl < 0 && ttl * -1 <= timeout);
    }

    @Test
    public void testSaveWithoutTTL() {
        // Arrange
        String value = "testValue";

        // Act
        redisDao.save(TEST_KEY, value);
        Object result = redisDao.get(TEST_KEY);

        // Assert
        assertEquals(value, result);
    }

    @Test
    public void testExist() {
        // Arrange
        redisDao.save(TEST_KEY, "value");

        // Act & Assert
        assertTrue(redisDao.exist(TEST_KEY));
        assertFalse(redisDao.exist("nonExistentKey"));
    }

    @Test
    public void testUpdateValueWithoutChangingTTL() throws InterruptedException {
        // Arrange
        String initialValue = "initial";
        String newValue = "updated";
        redisDao.save(TEST_KEY, initialValue, 10L, TimeUnit.SECONDS);
        Thread.sleep(1000); // Wait 1 second to ensure TTL decreases

        // Act
        Long initialTTL = redisTemplate.getExpire(TEST_KEY, TimeUnit.SECONDS);
        redisDao.updateValueWithoutChangingTTL(TEST_KEY, newValue);
        Long updatedTTL = redisTemplate.getExpire(TEST_KEY, TimeUnit.SECONDS);

        // Assert
        assertEquals(newValue, redisDao.get(TEST_KEY));
        assertTrue(updatedTTL < 0 && updatedTTL * -1 >= initialTTL); // TTL preserved, slightly reduced due to time elapsed
    }

    @Test
    public void testSaveAndGetHashSet() {
        // Arrange
        HashSet<Object> hashSet = new HashSet<>();
        hashSet.add("item1");
        hashSet.add("item2");

        // Act
        redisDao.saveHashSet(TEST_SET_KEY, hashSet);
        HashSet<Object> result = redisDao.getHashSet(TEST_SET_KEY);

        // Assert
        assertEquals(hashSet.size(), result.size());
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    @Test
    public void testIncr() {
        // Act
        Long result1 = redisDao.incr(TEST_KEY);
        Long result2 = redisDao.incr(TEST_KEY);

        // Assert

        assertEquals(1L, result1);
        assertEquals(2L, result2);
    }

    @Test
    public void testExpire() throws InterruptedException {
        // Arrange
        redisDao.save(TEST_KEY, "value");

        // Act
        redisDao.expire(TEST_KEY, 1L, TimeUnit.SECONDS);
        Thread.sleep(1500); // Wait for expiration

        // Assert
        assertNull(redisDao.get(TEST_KEY));
    }

    @Test
    public void testDelete() {
        // Arrange
        redisDao.save(TEST_KEY, "value");

        // Act
        Boolean deleted = redisDao.delete(TEST_KEY);

        // Assert
        assertTrue(deleted);
        assertNull(redisDao.get(TEST_KEY));
    }

    @Test
    public void testDeleteByPattern() {
        // Arrange
        redisDao.save("prefix:test1", "value1");
        redisDao.save("prefix:test2", "value2");
        redisDao.save("other:key", "value3");

        // Act
        redisDao.deleteByPattern("prefix:test*");

        // Assert
        assertNull(redisDao.get("prefix:test1"));
        assertNull(redisDao.get("prefix:test2"));
        assertEquals("value3", redisDao.get("other:key"));
    }

    @Test
    public void testAcquireAndReleaseLock() throws InterruptedException {
        // Act: Acquire lock
        boolean acquired = redisDao.acquireLock(LOCK_KEY, 5L, TimeUnit.SECONDS);

        // Assert: Lock acquired
        assertTrue(acquired);
        assertEquals("LOCKED", redisDao.get(LOCK_KEY));
        assertTrue(redisTemplate.getExpire(LOCK_KEY, TimeUnit.SECONDS) < 0);

        // Act: Try to acquire again (should fail)
        boolean secondAcquire = redisDao.acquireLock(LOCK_KEY, 2L, TimeUnit.SECONDS);
        assertFalse(secondAcquire);

        // Act: Release lock
        redisDao.releaseLock(LOCK_KEY);

        // Assert: Lock released
        assertNull(redisDao.get(LOCK_KEY));

        // Act: Acquire again after release
        boolean reacquired = redisDao.acquireLock(LOCK_KEY, 2L, TimeUnit.SECONDS);
        assertTrue(reacquired);
    }
}