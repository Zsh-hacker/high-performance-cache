package com.cache;

import com.zsh.cache.ExpiringCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ExpiringCacheTest {
    private ExpiringCache<String, String> cache;

    @AfterEach
    void tearDown() {
        cache.shutdown();
    }

    @BeforeEach
    void setUp() {
        cache = new ExpiringCache<>(1000, 500, TimeUnit.MILLISECONDS);
    }

    @Test
    void testBasicPutAndGet() {
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void testExpiration() throws InterruptedException {
        cache.put("key", "value");
        assertNotNull(cache.get("key"), "数据应该存在");

        Thread.sleep(1100);
        assertNull(cache.get("key"), "数据应该已过期");
    }

    @Test
    void testCustomExpireTime() throws InterruptedException {
        cache.put("key", "value", 2000);
        Thread.sleep(1000);
        assertNotNull(cache.get("key"), "数据应该还存在");

        Thread.sleep(1100);
        assertNull(cache.get("key"), "数据应该已过期");
    }

    @Test
    void testAutoCleanup() throws InterruptedException {
        cache.put("key", "value", 500);
        cache.put("key2", "value2", 800);

        assertEquals(2, cache.size());

        Thread.sleep(600);

        assertEquals(1, cache.size(), "应该只剩1个未过期的数据");
        assertNull(cache.get("key"), "key应该已被自动清理");
        assertNotNull(cache.get("key2"), "key2应该未过期");

        Thread.sleep(400);
        assertEquals(0, cache.size(), "所有数据应该已过期并被清理");
    }

    @Test
    void testLazyExpiration() {
        cache.put("key", "value", 100);

        assertNotNull(cache.get("key"));

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertNull(cache.get("key"), "惰性删除");
        assertEquals(0, cache.validSize());
    }

    @Test
    void testUpdateExtendsExpiration() throws InterruptedException {
        // 放入数据，过期时间500毫秒
        cache.put("key1", "value1", 500);

        // 300毫秒后更新值，使用新的过期时间
        Thread.sleep(300);
        cache.put("key1", "value2", 500);  // 重新设置500毫秒过期

        // 再等300毫秒（总共600毫秒），如果没有更新应该已过期
        // 但因为更新了，所以应该还存在
        Thread.sleep(300);
        assertEquals("value2", cache.get("key1"), "更新应该延长过期时间");
    }

    @Test
    void testInvalidExpireTime() {
        // 测试无效的过期时间
        assertThrows(IllegalArgumentException.class, () -> {
            cache.put("key", "value", 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cache.put("key", "value", -1);
        });

        // 构造方法也要检查
        assertThrows(IllegalArgumentException.class, () -> {
            new ExpiringCache<>(0);
        });
    }

    @Test
    void testShutdown() throws InterruptedException {
        cache.put("key1", "value1", 100);

        // 关闭缓存
        cache.shutdown();

        // 关闭后不能再自动清理
        // 但已有的数据还能获取（如果未过期）
        assertNotNull(cache.get("key1"));

        // 等待数据过期
        Thread.sleep(150);

        // 由于清理线程已停止，惰性删除仍然有效
        assertNull(cache.get("key1"));
    }

}
