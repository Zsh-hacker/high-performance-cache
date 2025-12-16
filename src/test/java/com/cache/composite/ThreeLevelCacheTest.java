package com.cache.composite;

import com.zsh.cache.Cache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.LRUCache;
import com.zsh.cache.ConcurrentCache;
import com.zsh.cache.composite.ThreeLevelCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

/**
 * 三级缓存测试
 */
class ThreeLevelCacheTest {

    private ThreeLevelCache<String, String> threeLevelCache;
    private Cache<String, String> l1;
    private Cache<String, String> l2;
    private Cache<String, String> l3;

    @BeforeEach
    void setUp() {
        // L1: 快速小容量缓存
        l1 = new LRUCache<>(10);

        // L2: 中等容量缓存
        l2 = new LRUCache<>(100);

        // L3: 大容量缓存
        l3 = new SimpleCache<>();

        threeLevelCache = new ThreeLevelCache<>(l1, l2, l3, null);
    }

    @Test
    void testThreeLevelReadThrough() {
        // 在L3放入数据
        l3.put("key1", "value1");

        // 第一次获取：L1未命中，L2未命中，L3命中
        assertEquals("value1", threeLevelCache.get("key1"));

        // 验证统计
        assertEquals(0, threeLevelCache.getL1HitCount());
        assertEquals(0, threeLevelCache.getL2HitCount());
        assertEquals(1, threeLevelCache.getL3HitCount());

        // 验证回填
        assertNotNull(l1.get("key1"));  // 应该回填到L1
        assertNotNull(l2.get("key1"));  // 应该回填到L2

        // 第二次获取：L1应该命中
        assertEquals("value1", threeLevelCache.get("key1"));
        assertEquals(1, threeLevelCache.getL1HitCount());
    }

    @Test
    void testWriteThrough() {
        // 写入三级缓存
        threeLevelCache.put("key1", "value1");

        // 验证所有层级都有数据
        assertEquals("value1", l1.get("key1"));
        assertEquals("value1", l2.get("key1"));
        assertEquals("value1", l3.get("key1"));
    }

    @Test
    void testCacheHierarchy() {
        // 测试缓存的层次结构

        // 只在L3有数据
        l3.put("key1", "value1");

        // 第一次获取：L3命中
        threeLevelCache.get("key1");
        assertEquals(1, threeLevelCache.getL3HitCount());

        // 第二次获取：L1命中（已回填）
        threeLevelCache.get("key1");
        assertEquals(1, threeLevelCache.getL1HitCount());

        // 在L2放入新数据
        l2.put("key2", "value2");

        // 获取：L1未命中，L2命中
        threeLevelCache.get("key2");
        assertEquals(1, threeLevelCache.getL2HitCount());
        assertNotNull(l1.get("key2"));  // 应该回填到L1

        System.out.println("三级缓存统计:");
        threeLevelCache.printStats();
    }

    @Test
    void testPreload() {
        // 在L3准备数据
        for (int i = 0; i < 5; i++) {
            l3.put("key" + i, "value" + i);
        }

        // 预加载前3个key到上层
        threeLevelCache.preloadToUpperLevels(
                Arrays.asList("key0", "key1", "key2"));

        // 验证L1和L2有这些数据
        assertNotNull(l1.get("key0"));
        assertNotNull(l1.get("key1"));
        assertNotNull(l1.get("key2"));
        assertNull(l1.get("key3"));  // 未预加载

        assertNotNull(l2.get("key0"));
        assertNotNull(l2.get("key1"));
        assertNotNull(l2.get("key2"));
        assertNull(l2.get("key3"));

        System.out.println("预加载测试完成");
    }

    @Test
    void testDemote() {
        // 在L1放入数据
        l1.put("hotKey", "hotValue");

        // demote前验证
        assertNotNull(l1.get("hotKey"));
        assertNull(l2.get("hotKey"));
        assertNull(l3.get("hotKey"));

        // 执行降级
        threeLevelCache.demoteToLowerLevels("hotKey");

        // demote后验证
        assertNull(l1.get("hotKey"));      // 从L1移除
        assertNotNull(l2.get("hotKey"));   // 写入L2
        assertNotNull(l3.get("hotKey"));   // 写入L3

        // 再次获取应该从L3获取并回填
        threeLevelCache.get("hotKey");
        assertNotNull(l1.get("hotKey"));   // 重新回填到L1
    }

    @Test
    void testLoader() {
        // 创建带加载器的三级缓存
        ThreeLevelCache<String, String> cacheWithLoader =
                new ThreeLevelCache<>(l1, l2, l3, key -> "loaded-" + key);

        // 获取不存在的数据
        String value = cacheWithLoader.get("newKey");
        assertEquals("loaded-newKey", value);
        assertEquals(1, cacheWithLoader.getLoaderCallCount());

        // 验证数据被写入所有层级
        assertEquals("loaded-newKey", l1.get("newKey"));
        assertEquals("loaded-newKey", l2.get("newKey"));
        assertEquals("loaded-newKey", l3.get("newKey"));
    }

    @Test
    void testStatsAccuracy() {
        // 验证统计的准确性

        // 准备测试数据
        l3.put("key1", "value1");
        l2.put("key2", "value2");
        l1.put("key3", "value3");

        // 执行各种获取操作
        threeLevelCache.get("key1");  // L3命中
        threeLevelCache.get("key2");  // L2命中
        threeLevelCache.get("key3");  // L1命中
        threeLevelCache.get("key4");  // 全部未命中

        // 验证统计
        assertEquals(1, threeLevelCache.getL1HitCount());
        assertEquals(1, threeLevelCache.getL2HitCount());
        assertEquals(1, threeLevelCache.getL3HitCount());
        assertEquals(4, threeLevelCache.getTotalReadCount());

        // 验证命中率
        assertEquals(0.25, threeLevelCache.getL1HitRate(), 0.001);
        assertEquals(0.25, threeLevelCache.getL2HitRate(), 0.001);
        assertEquals(0.25, threeLevelCache.getL3HitRate(), 0.001);
        assertEquals(0.75, threeLevelCache.getOverallHitRate(), 0.001);

        System.out.println("统计准确性测试:");
        threeLevelCache.printStats();
    }

    @Test
    void testInvalidConstructor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ThreeLevelCache<>(null, l2, l3, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ThreeLevelCache<>(l1, null, l3, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ThreeLevelCache<>(l1, l2, null, null);
        });
    }
}