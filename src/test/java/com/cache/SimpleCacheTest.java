package com.cache;

import com.zsh.cache.SimpleCache;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleCacheTest {
    private SimpleCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new SimpleCache<>();
    }

    /**
     * 测试基本的put和get操作
     */
    @Test
    void testBasicPutAndGet() {
        String key = "name";
        String value = "zsh";

        cache.put(key, value);

        String result = cache.get(key);
        assertEquals(value, result, "获取的值应该与存入的值相同");
    }

    /**
     * 测试获取不存在的键
     */
    @Test
    void testGetNonExistenKey() {
        assertNull(cache.get("nonExistentKey"), "获取不存在的键应该返回null");
    }

    /**
     * 测试存储null值
     */
    @Test
    void TestPutNullValue() {
        String key = "nullValue";
        cache.put(key, null);
        assertNull(cache.get(key), "存储的null值应该可以获取到");
    }

    /**
     * 测试存储null键
     */
    @Test
    void testPutNullKey() {
        cache.put(null, "value");
        assertEquals("value", cache.get(null), "null键应该可以正常存储和获取");
    }

    /**
     * 测试移除操作
     */
    @Test
    void testRemove() {
        cache.put("key", "value");
        assertEquals(1, cache.size(), "存入后大小应该为1");

        cache.remove("key");
        assertEquals(0, cache.size(), "移除后大小应该为0");
        assertNull(cache.get("key"), "移除后获取应该返回null");
    }

    /**
     * 测试清空操作
     */
    @Test
    void testClear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertEquals(2, cache.size(), "存入后大小应该为2");

        cache.clear();
        assertEquals(0, cache.size(), "清空后大小应该为0");
        assertTrue(cache.isEmpty(), "清空后应该为空");
    }

    /**
     * 测试大小统计
     */
    @Test
    void testSize() {
        assertEquals(0, cache.size(), "初始大小应该为0");
        cache.put("key1", "value1");
        assertEquals(1, cache.size(), "存入1个数据后大小应该为1");

        cache.put("key1", "newValue");
        assertEquals(1, cache.size(), "覆盖已有键不应该改变大小");

        cache.put("key2", "value2");
        assertEquals(2, cache.size(), "存入第2个数据后大小应该为2");
    }

    @Test

    void testIsEmpty() {
        assertTrue(cache.isEmpty(), "初始缓存应该为空");

        cache.put("key", "value");
        assertFalse(cache.isEmpty(), "存入数据后缓存不应该为空");

        cache.remove("key");
        assertTrue(cache.isEmpty(), "移除所有数据后缓存应该为空");
    }

    @Test
    void testContainsKey() {
        String key = "testKey";
        assertFalse(cache.containsKey(key), "初始状态不应该包含该键");

        cache.put(key, "value");
        assertTrue(cache.containsKey(key), "存入数据后应该包含该键");

        cache.remove(key);
        assertFalse(cache.containsKey(key), "移除数据后不应该包含该键");
    }

}
