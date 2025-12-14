package com.cache.decorator;

import com.zsh.cache.Cache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.decorator.CacheWithStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CacheWithStatsTest {

    private CacheWithStats<String , String> cacheWithStats;
    private Cache<String, String> delegateCache;

    @BeforeEach
    void setUp() {
        delegateCache = new SimpleCache<>();
        cacheWithStats = new CacheWithStats<>(delegateCache);
    }

    @Test
    void testBasicFunctionality() {
        // 测试基本功能是否正常
        cacheWithStats.put("key", "value");
        assertEquals("value", cacheWithStats.get("key"));
        assertEquals(1, cacheWithStats.size());
    }

    @Test
    void testHitMissStatistics() {
        // 测试命中/未命中统计
        cacheWithStats.put("key1", "value1");

        // 第一次获取：命中
        cacheWithStats.get("key1");
        assertEquals(1, cacheWithStats.getHitCount());
        assertEquals(0, cacheWithStats.getMissCount());
        assertEquals(1.0, cacheWithStats.getHitRate(), 0.001);

        // 获取不存在的key：未命中
        cacheWithStats.get("nonexistent");
        assertEquals(1, cacheWithStats.getHitCount());
        assertEquals(1, cacheWithStats.getMissCount());
        assertEquals(0.5, cacheWithStats.getHitRate(), 0.001);
    }

    @Test
    void testPutRemoveStatistics() {
        cacheWithStats.put("key1", "value1");
        assertEquals(1, cacheWithStats.getPutCount());

        cacheWithStats.put("key2", "value2");
        assertEquals(2, cacheWithStats.getPutCount());

        cacheWithStats.remove("key1");
        assertEquals(1, cacheWithStats.getRemoveCount());
    }

    @Test
    void testTimeStatistics() throws InterruptedException {
        // 创建一个慢缓存用于测试时间统计
        Cache<String, String> slowCache = new Cache<String, String>() {
            @Override
            public String get(String key) {
                try {
                    Thread.sleep(10);  // 模拟慢操作
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "value";
            }

            @Override
            public void put(String key, String value) {
                try {
                    Thread.sleep(20);  // 模拟慢操作
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override public void remove(String key) {}
            @Override public void clear() {}
            @Override public int size() { return 1; }
        };

        CacheWithStats<String, String> slowStats = new CacheWithStats<>(slowCache);

        slowStats.get("test");
        slowStats.put("test", "value");

        // 验证时间统计不为0
        assertTrue(slowStats.getAverageGetTime() > 0, "平均获取时间应该大于0");
        assertTrue(slowStats.getAveragePutTime() > 0, "平均放入时间应该大于0");

        System.out.println("慢缓存统计:");
        slowStats.printStats();
    }

    @Test
    void testResetStats() {
        cacheWithStats.put("key1", "value1");
        cacheWithStats.get("key1");
        cacheWithStats.get("nonexistent");

        // 重置前应该有统计
        assertTrue(cacheWithStats.getHitCount() > 0);
        assertTrue(cacheWithStats.getMissCount() > 0);

        // 重置统计
        cacheWithStats.resetStats();

        // 重置后应该为0
        assertEquals(0, cacheWithStats.getHitCount());
        assertEquals(0, cacheWithStats.getMissCount());
        assertEquals(0, cacheWithStats.getPutCount());
        assertEquals(0, cacheWithStats.getRemoveCount());
        assertEquals(0, cacheWithStats.getAverageGetTime(), 0.001);
        assertEquals(0, cacheWithStats.getAveragePutTime(), 0.001);
    }

    @Test
    void testStatsString() {
        cacheWithStats.put("key1", "value1");
        cacheWithStats.get("key1");
        cacheWithStats.get("nonexistent");

        String stats = cacheWithStats.getStatsString();
        System.out.println("统计信息: " + stats);

        assertTrue(stats.contains("hits=1"));
        assertTrue(stats.contains("misses=1"));
        assertTrue(stats.contains("hitRate=50.00%"));
    }

    @Test
    void testNullDelegate() {
        // 测试空委托
        assertThrows(IllegalArgumentException.class, () -> {
            new CacheWithStats<>(null);
        });
    }

    @Test
    void testDecoratorChain() {
        // 测试装饰器链：可以多层装饰
        Cache<String, String> baseCache = new SimpleCache<>();
        CacheWithStats<String, String> statsCache = new CacheWithStats<>(baseCache);

        // 可以再装饰一层（比如加日志）
        Cache<String, String> loggingCache = new Cache<String, String>() {
            private final Cache<String, String> delegate = statsCache;

            @Override
            public String get(String key) {
                System.out.println("Getting key: " + key);
                return delegate.get(key);
            }

            @Override
            public void put(String key, String value) {
                System.out.println("Putting key: " + key + ", value: " + value);
                delegate.put(key, value);
            }

            @Override public void remove(String key) { delegate.remove(key); }
            @Override public void clear() { delegate.clear(); }
            @Override public int size() { return delegate.size(); }
        };

        // 使用装饰器链
        loggingCache.put("test", "value");
        loggingCache.get("test");

        // 仍然可以获取统计信息
        statsCache.printStats();
    }
}
