package com.cache.factory;

import com.zsh.cache.Cache;
import com.zsh.cache.decorator.CacheWithStats;
import com.zsh.cache.composite.TwoLevelCache;
import com.zsh.cache.factory.CacheFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存工厂测试
 */
class CacheFactoryTest {

    @Test
    void testCreateSimpleCache() {
        Cache<String, String> cache = CacheFactory.createSimpleCache();
        assertNotNull(cache);

        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void testCreateSynchronizedCache() {
        Cache<String, String> cache = CacheFactory.createSynchronizedCache();
        assertNotNull(cache);

        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void testCreateConcurrentCache() {
        Cache<String, String> cache = CacheFactory.createConcurrentCache();
        assertNotNull(cache);

        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void testCreateLRUCache() {
        Cache<String, String> cache = CacheFactory.createLRUCache(10);
        assertNotNull(cache);

        // 测试LRU行为
        for (int i = 0; i < 15; i++) {
            cache.put("key" + i, "value" + i);
        }

        // 容量为10，应该有淘汰
        assertTrue(cache.size() <= 10);
    }

    @Test
    void testCreateCacheWithStats() {
        Cache<String, String> baseCache = CacheFactory.createSimpleCache();
        Cache<String, String> statsCache = CacheFactory.createCacheWithStats(baseCache);

        assertNotNull(statsCache);
        assertTrue(statsCache instanceof CacheWithStats);

        statsCache.put("key", "value");
        statsCache.get("key");
        statsCache.get("nonexistent");

        // 可以强制转换获取统计信息
        CacheWithStats<String, String> cacheWithStats = (CacheWithStats<String, String>) statsCache;
        assertTrue(cacheWithStats.getHitCount() > 0);
    }

    @Test
    void testCreateLRUCacheWithStats() {
        Cache<String, String> cache = CacheFactory.createLRUCacheWithStats(10);
        assertNotNull(cache);

        cache.put("key", "value");
        cache.get("key");

        // 这是一个装饰器，内部是LRU缓存
        assertTrue(cache instanceof CacheWithStats);
    }

    @Test
    void testCreateTwoLevelCache() {
        Cache<String, String> l1 = CacheFactory.createSimpleCache();
        Cache<String, String> l2 = CacheFactory.createSimpleCache();

        Cache<String, String> twoLevelCache = CacheFactory.createTwoLevelCache(l1, l2);
        assertNotNull(twoLevelCache);
        assertTrue(twoLevelCache instanceof TwoLevelCache);

        twoLevelCache.put("key", "value");
        assertEquals("value", twoLevelCache.get("key"));
    }

    @Test
    void testCreateRecommendedCache() {
        Cache<String, String> cache = CacheFactory.createRecommendedCache(100, 1000);
        assertNotNull(cache);

        // 推荐缓存应该是二级缓存
        assertTrue(cache instanceof TwoLevelCache);

        cache.put("key", "value");
        assertEquals("value", cache.get("key"));
    }

    @Test
    void testCreateCacheFromConfig() {
        // 测试各种配置
        Cache<String, String> cache;

        cache = CacheFactory.createCacheFromConfig("simple");
        assertNotNull(cache);
        cache.put("key", "value");
        assertEquals("value", cache.get("key"));

        cache = CacheFactory.createCacheFromConfig("sync");
        assertNotNull(cache);

        cache = CacheFactory.createCacheFromConfig("concurrent");
        assertNotNull(cache);

        cache = CacheFactory.createCacheFromConfig("concurrent:100");
        assertNotNull(cache);

        cache = CacheFactory.createCacheFromConfig("lru:50");
        assertNotNull(cache);

        cache = CacheFactory.createCacheFromConfig("lru-stats:50");
        assertNotNull(cache);
        assertTrue(cache instanceof CacheWithStats);
    }

    @Test
    void testInvalidConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            CacheFactory.createCacheFromConfig("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CacheFactory.createCacheFromConfig(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CacheFactory.createCacheFromConfig("invalid-type");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CacheFactory.createCacheFromConfig("lru");  // 缺少容量
        });
    }

    @Test
    void testFactoryUsageExample() {
        System.out.println("=== 缓存工厂使用示例 ===");

        // 示例1：创建生产环境推荐的缓存
        Cache<String, String> recommendedCache =
                CacheFactory.createRecommendedCache(100, 1000);
        System.out.println("1. 创建推荐缓存: " + recommendedCache.getClass().getSimpleName());

        // 示例2：根据配置创建缓存
        Cache<String, String> configuredCache =
                CacheFactory.createCacheFromConfig("lru-stats:50");
        System.out.println("2. 根据配置创建: " + configuredCache.getClass().getSimpleName());

        // 示例3：创建带统计的缓存
        Cache<String, String> simpleCache = CacheFactory.createSimpleCache();
        Cache<String, String> statsCache = CacheFactory.createCacheWithStats(simpleCache);
        System.out.println("3. 创建带统计的缓存: " + statsCache.getClass().getSimpleName());

        // 示例4：使用缓存
        statsCache.put("name", "张三");
        statsCache.put("age", "30");

        System.out.println("4. 获取数据: name=" + statsCache.get("name"));
        System.out.println("5. 缓存大小: " + statsCache.size());

        // 打印统计
        if (statsCache instanceof CacheWithStats) {
            CacheWithStats<String, String> cacheWithStats = (CacheWithStats<String, String>) statsCache;
            cacheWithStats.printStats();
        }
    }
}