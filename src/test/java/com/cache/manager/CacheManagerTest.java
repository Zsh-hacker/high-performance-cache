package com.cache.manager;

import com.zsh.cache.Cache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.LRUCache;
import com.zsh.cache.decorator.CacheWithStats;
import com.zsh.cache.manager.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;

/**
 * 缓存管理器测试
 */
class CacheManagerTest {

    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = CacheManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cacheManager.clearAllCaches();
        Set<String> cacheNames = cacheManager.getAllCacheNames();
        for (String name : cacheNames) {
            cacheManager.removeCache(name);
        }
    }

    @Test
    void testSingleton() {
        CacheManager anotherInstance = CacheManager.getInstance();
        assertSame(cacheManager, anotherInstance, "应该是同一个实例");
    }

    @Test
    void testRegisterAndGetCache() {
        Cache<String, String> cache = new SimpleCache<>();

        // 注册缓存
        cacheManager.registerCache("userCache", cache);

        // 获取缓存
        Cache<String, String> retrieved = cacheManager.getCache("userCache");
        assertSame(cache, retrieved);

        // 使用缓存
        retrieved.put("user1", "张三");
        assertEquals("张三", retrieved.get("user1"));
    }

    @Test
    void testMultipleCaches() {
        // 注册多个不同类型的缓存
        Cache<String, String> userCache = new SimpleCache<>();
        Cache<Integer, String> productCache = new LRUCache<>(100);
        Cache<String, Double> priceCache = new SimpleCache<>();

        cacheManager.registerCache("users", userCache);
        cacheManager.registerCache("products", productCache);
        cacheManager.registerCache("prices", priceCache);

        // 验证缓存数量
        assertEquals(3, cacheManager.getAllCacheNames().size());

        // 验证可以获取不同类型的缓存
        assertNotNull(cacheManager.getCache("users"));
        assertNotNull(cacheManager.getCache("products"));
        assertNotNull(cacheManager.getCache("prices"));

        // 使用不同类型的缓存
        userCache.put("id1", "张三");
        productCache.put(1001, "手机");
        priceCache.put("iphone", 6999.99);

        assertEquals("张三", userCache.get("id1"));
        assertEquals("手机", productCache.get(1001));
        assertEquals(6999.99, priceCache.get("iphone"));
    }

    @Test
    void testRemoveCache() {
        Cache<String, String> cache = new SimpleCache<>();
        cacheManager.registerCache("testCache", cache);

        assertNotNull(cacheManager.getCache("testCache"));

        cacheManager.removeCache("testCache");

        assertNull(cacheManager.getCache("testCache"));
    }

    @Test
    void testClearAllCaches() {
        // 创建并注册多个缓存
        Cache<String, String> cache1 = new SimpleCache<>();
        Cache<String, String> cache2 = new SimpleCache<>();

        cache1.put("key1", "value1");
        cache2.put("key2", "value2");

        cacheManager.registerCache("cache1", cache1);
        cacheManager.registerCache("cache2", cache2);

        // 验证缓存有数据
        assertEquals("value1", cache1.get("key1"));
        assertEquals("value2", cache2.get("key2"));

        // 清空所有缓存
        cacheManager.clearAllCaches();

        // 验证缓存已清空
        assertNull(cache1.get("key1"));
        assertNull(cache2.get("key2"));
    }

    @Test
    void testCacheStats() {
        // 创建带统计的缓存
        Cache<String, String> baseCache = new SimpleCache<>();
        CacheWithStats<String, String> statsCache = new CacheWithStats<>(baseCache);

        cacheManager.registerCache("statsCache", statsCache);

        // 执行一些操作
        statsCache.put("key1", "value1");
        statsCache.put("key2", "value2");
        statsCache.get("key1");  // 命中
        statsCache.get("key2");  // 命中
        statsCache.get("nonexistent");  // 未命中

        // 获取统计信息
        Map<String, CacheManager.CacheStats> stats = cacheManager.getCacheStats();

        assertTrue(stats.containsKey("statsCache"));
        CacheManager.CacheStats cacheStats = stats.get("statsCache");

        assertEquals(2, cacheStats.hits);
        assertEquals(1, cacheStats.misses);
        assertEquals(2.0/3.0, cacheStats.hitRate, 0.001);
        assertEquals(2, cacheStats.size);

        System.out.println("缓存统计测试:");
        System.out.printf("命中: %d, 未命中: %d, 命中率: %.1f%%, 大小: %d%n",
                cacheStats.hits, cacheStats.misses, cacheStats.hitRate * 100, cacheStats.size);
    }

    @Test
    void testPrintAllStats() {
        // 创建多个带统计的缓存
        CacheWithStats<String, String> cache1 = new CacheWithStats<>(new SimpleCache<>());
        CacheWithStats<String, String> cache2 = new CacheWithStats<>(new LRUCache<>(10));

        cacheManager.registerCache("UserCache", cache1);
        cacheManager.registerCache("ProductCache", cache2);

        // 添加一些数据
        cache1.put("user1", "张三");
        cache1.put("user2", "李四");
        cache1.get("user1");
        cache1.get("user3");  // 未命中

        cache2.put("product1", "手机");
        cache2.put("product2", "电脑");
        cache2.get("product1");
        cache2.get("product2");

        System.out.println("=== 打印所有缓存统计 ===");
        cacheManager.printAllStats();
    }

    @Test
    void testInvalidRegistration() {
        assertThrows(IllegalArgumentException.class, () -> {
            cacheManager.registerCache(null, new SimpleCache<>());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cacheManager.registerCache("", new SimpleCache<>());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            cacheManager.registerCache("test", null);
        });
    }

    @Test
    void testCacheManagerIntegration() throws InterruptedException {
        System.out.println("=== 缓存管理器集成测试 ===");

        // 创建不同类型的缓存
        CacheWithStats<String, String> userCache =
                new CacheWithStats<>(new SimpleCache<>());
        CacheWithStats<Integer, String> productCache =
                new CacheWithStats<>(new LRUCache<>(50));

        // 注册到管理器
        cacheManager.registerCache("UserCache", userCache);
        cacheManager.registerCache("ProductCache", productCache);

        // 模拟业务操作
        userCache.put("1001", "张三");
        userCache.put("1002", "李四");
        userCache.put("1003", "王五");

        productCache.put(2001, "iPhone 15");
        productCache.put(2002, "MacBook Pro");
        productCache.put(2003, "iPad Air");

        // 模拟访问
        userCache.get("1001");
        userCache.get("1002");
        userCache.get("9999");  // 未命中

        productCache.get(2001);
        productCache.get(2002);
        productCache.get(2003);
        productCache.get(9999);  // 未命中

        // 打印统计
        cacheManager.printAllStats();

        // 测试监控（短暂运行）
        cacheManager.startMonitoring(2);
        Thread.sleep(4500);  // 等待2个报告周期
        cacheManager.stopMonitoring();

        System.out.println("集成测试完成");
    }

    @Test
    void testShutdown() {
        Cache<String, String> cache = new SimpleCache<>();
        cache.put("key", "value");

        cacheManager.registerCache("test", cache);

        assertNotNull(cacheManager.getCache("test"));
        assertEquals("value", cache.get("key"));

        // 关闭管理器
        cacheManager.shutdown();

        // 缓存应该被清空
        assertNull(cache.get("key"));

        // 管理器应该不再有缓存
        assertEquals(0, cacheManager.getAllCacheNames().size());
    }
}