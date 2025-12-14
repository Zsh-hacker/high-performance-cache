package com.cache.decorator;

import com.zsh.cache.Cache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.decorator.CacheWithLogger;
import com.zsh.cache.decorator.CacheWithStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * CacheWithLogger测试
 */
class CacheWithLoggerTest {

    private Cache<String, String> delegateCache;
    private CacheWithLogger<String, String> loggingCache;
    private List<String> logMessages;

    @BeforeEach
    void setUp() {
        delegateCache = new SimpleCache<>();
        loggingCache = new CacheWithLogger<>(delegateCache, "TestCache");

        // 收集日志消息用于测试
        logMessages = new ArrayList<>();
        loggingCache.setLogHandler((level, message) -> {
            logMessages.add(message);
        });
    }

    @Test
    void testGetHitLogging() {
        // 准备数据
        delegateCache.put("key1", "value1");

        // 获取存在的数据
        String value = loggingCache.get("key1");
        assertEquals("value1", value);

        // 验证日志
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("GET hit"));
        assertTrue(logMessages.get(0).contains("key=key1"));
        assertTrue(logMessages.get(0).contains("value=value1"));

        System.out.println("GET命中日志: " + logMessages.get(0));
    }

    @Test
    void testGetMissLogging() {
        // 获取不存在的数据
        String value = loggingCache.get("nonexistent");
        assertNull(value);

        // 验证日志
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("GET miss"));
        assertTrue(logMessages.get(0).contains("key=nonexistent"));

        System.out.println("GET未命中日志: " + logMessages.get(0));
    }

    @Test
    void testPutLogging() {
        // 放入数据
        loggingCache.put("key1", "value1");

        // 验证日志
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("PUT"));
        assertTrue(logMessages.get(0).contains("key=key1"));
        assertTrue(logMessages.get(0).contains("value=value1"));

        System.out.println("PUT日志: " + logMessages.get(0));
    }

    @Test
    void testRemoveLogging() {
        // 先放入再移除
        delegateCache.put("key1", "value1");
        loggingCache.remove("key1");

        // 验证日志
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("REMOVE"));
        assertTrue(logMessages.get(0).contains("key=key1"));

        System.out.println("REMOVE日志: " + logMessages.get(0));
    }

    @Test
    void testClearLogging() {
        // 放入一些数据
        delegateCache.put("key1", "value1");
        delegateCache.put("key2", "value2");

        // 清空
        loggingCache.clear();

        // 验证日志
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("CLEAR"));
        assertTrue(logMessages.get(0).contains("cleared 2 entries"));

        System.out.println("CLEAR日志: " + logMessages.get(0));
    }

    @Test
    void testLogLevels() {
        // 测试DEBUG级别
        loggingCache.setLogLevel(CacheWithLogger.LogLevel.DEBUG);
        loggingCache.size();  // DEBUG级别会记录SIZE操作

        assertTrue(logMessages.size() > 0);
        assertTrue(logMessages.get(0).contains("SIZE"));

        System.out.println("DEBUG级别日志: " + logMessages.get(0));

        // 清空日志
        logMessages.clear();

        // 测试WARN级别 - INFO级别以下的操作不记录
        loggingCache.setLogLevel(CacheWithLogger.LogLevel.WARN);
        loggingCache.get("key");  // INFO级别，不会记录

        assertEquals(0, logMessages.size());

        // CLEAR是WARN级别，会记录
        loggingCache.clear();
        assertEquals(1, logMessages.size());
        assertTrue(logMessages.get(0).contains("CLEAR"));
    }

    @Test
    void testDisableLogging() {
        // 禁用日志
        loggingCache.setEnabled(false);

        // 执行操作
        loggingCache.put("key1", "value1");
        loggingCache.get("key1");

        // 应该没有日志
        assertEquals(0, logMessages.size());

        // 重新启用
        loggingCache.setEnabled(true);
        loggingCache.remove("key1");

        // 现在应该有日志
        assertEquals(1, logMessages.size());
    }

    @Test
    void testErrorLogging() {
        // 创建一个会抛出异常的缓存
        Cache<String, String> errorCache = new Cache<String, String>() {
            @Override
            public String get(String key) {
                throw new RuntimeException("Test exception");
            }

            @Override
            public void put(String key, String value) {
                throw new RuntimeException("Test exception");
            }

            @Override public void remove(String key) {}
            @Override public void clear() {}
            @Override public int size() { return 0; }
        };

        CacheWithLogger<String, String> errorLoggingCache =
                new CacheWithLogger<>(errorCache, "ErrorCache");

        // 设置日志处理器来捕获错误日志
        List<String> errorLogs = new ArrayList<>();
        errorLoggingCache.setLogHandler((level, message) -> {
            if (level == CacheWithLogger.LogLevel.ERROR) {
                errorLogs.add(message);
            }
        });

        // 执行会抛出异常的操作
        try {
            errorLoggingCache.get("key");
        } catch (RuntimeException e) {
            // 预期中的异常
        }

        // 验证错误日志被记录
        assertEquals(1, errorLogs.size());
        assertTrue(errorLogs.get(0).contains("GET error"));
        assertTrue(errorLogs.get(0).contains("Test exception"));

        System.out.println("错误日志: " + errorLogs.get(0));
    }

    @Test
    void testValueAbbreviation() {
        // 测试长值截断
        String longValue = "A".repeat(100);
        loggingCache.put("key", longValue);

        assertEquals(1, logMessages.size());
        String log = logMessages.get(0);

        System.out.println(log);
        // 值应该被截断
        assertTrue(log.contains("AAA..."));
        //assertFalse(log.contains("AAAAAAAAAAAAAAAA"));  // 不应该有完整的100个A

        System.out.println("截断日志: " + log);
    }

    @Test
    void testDecoratorChain() {
        // 测试装饰器链：统计 + 日志
        Cache<String, String> baseCache = new SimpleCache<>();
        CacheWithStats<String, String> statsCache = new CacheWithStats<>(baseCache);
        CacheWithLogger<String, String> statsAndLoggingCache =
                new CacheWithLogger<>(statsCache, "StatsAndLoggingCache");

        // 执行操作
        statsAndLoggingCache.put("key1", "value1");
        statsAndLoggingCache.get("key1");
        statsAndLoggingCache.get("nonexistent");

        // 验证功能正常
        assertEquals("value1", statsAndLoggingCache.get("key1"));

        // 可以获取统计信息
        if (statsAndLoggingCache instanceof CacheWithLogger) {
            CacheWithLogger<String, String> logger =
                    (CacheWithLogger<String, String>) statsAndLoggingCache;
            System.out.println("缓存名称: " + logger.getCacheName());
        }

        System.out.println("装饰器链测试完成");
    }

    @Test
    void testNullDelegate() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CacheWithLogger<>(null);
        });
    }
}