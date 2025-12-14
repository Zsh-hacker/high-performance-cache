package com.cache.composite;

import com.zsh.cache.Cache;
import com.zsh.cache.LRUCache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.decorator.composite.TwoLevelCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class TwoLevelCacheTest {
    private Cache<String, String> l1Cache;
    private Cache<String, String> l2Cache;
    private TwoLevelCache<String, String> twoLevelCache;

    @BeforeEach
    void setUp() {
        // L1: 容量小的LRU缓存
        l1Cache = new LRUCache<>(2);

        // L2: 容量大的简单缓存
        l2Cache = new SimpleCache<>();

        // 创建二级缓存（无加载器）
        twoLevelCache = new TwoLevelCache<>(l1Cache, l2Cache);
    }

    @Test
    void testBasicReadThrough() {
        // 先在L2放入数据
        l2Cache.put("key1", "value1");

        // 第一次获取：L1未命中，L2命中，回填L1
        assertEquals("value1", twoLevelCache.get("key1"));

        // 验证统计
        assertEquals(0, twoLevelCache.getL1HitCount());  // 第一次是L2命中
        assertEquals(1, twoLevelCache.getL2HitCount());
        assertEquals(1, twoLevelCache.getTotalReadCount());

        // 第二次获取：L1应该命中
        assertEquals("value1", twoLevelCache.get("key1"));
        assertEquals(1, twoLevelCache.getL1HitCount());  // 现在L1命中
    }

    @Test
    void testWriteThrough() {
        // 写入二级缓存
        twoLevelCache.put("key1", "value1");

        // 验证两级缓存都有数据
        assertEquals("value1", l1Cache.get("key1"));
        assertEquals("value1", l2Cache.get("key1"));
    }

    @Test
    void testCacheLoader() {
        // 创建带加载器的二级缓存
        Function<String, String> loader = key -> {
            System.out.println("加载器被调用，key: " + key);
            return "loaded-" + key;
        };

        TwoLevelCache<String, String> cacheWithLoader =
                new TwoLevelCache<>(l1Cache, l2Cache, loader);

        // 获取不存在的数据，应该调用加载器
        String value = cacheWithLoader.get("newKey");
        assertEquals("loaded-newKey", value);
        assertEquals(1, cacheWithLoader.getLoaderCallCount());

        // 验证数据被写入两级缓存
        assertEquals("loaded-newKey", l1Cache.get("newKey"));
        assertEquals("loaded-newKey", l2Cache.get("newKey"));
    }

    @Test
    void testL1EvictionAndBackfill() {
        // L1容量为2，测试淘汰和回填

        // 放入3个数据到L2
        l2Cache.put("key1", "value1");
        l2Cache.put("key2", "value2");
        l2Cache.put("key3", "value3");

        // 获取key1和key2，它们应该进入L1
        twoLevelCache.get("key1");
        twoLevelCache.get("key2");

        assertEquals(2, l1Cache.size());  // L1已满

        // 获取key3，应该从L2获取
        twoLevelCache.get("key3");

        // 现在L1应该有key2和key3（LRU淘汰了key1）
        assertNull(l1Cache.get("key1"));  // key1被淘汰
        assertNotNull(l1Cache.get("key2"));
        assertNotNull(l1Cache.get("key3"));
    }

    @Test
    void testRemove() {
        twoLevelCache.put("key1", "value1");

        // 移除
        twoLevelCache.remove("key1");

        // 验证两级缓存都没有数据
        assertNull(l1Cache.get("key1"));
        assertNull(l2Cache.get("key1"));
    }

    @Test
    void testClear() {
        twoLevelCache.put("key1", "value1");
        twoLevelCache.put("key2", "value2");

        assertEquals(2, l1Cache.size());
        assertEquals(2, l2Cache.size());

        twoLevelCache.clear();

        assertEquals(0, l1Cache.size());
        assertEquals(0, l2Cache.size());
    }

    @Test
    void testStats() {
        // 准备数据
        l2Cache.put("key1", "value1");
        l2Cache.put("key2", "value2");

        // 多次访问
        twoLevelCache.get("key1");  // L2命中，回填L1
        twoLevelCache.get("key1");  // L1命中
        twoLevelCache.get("key2");  // L2命中，回填L1
        twoLevelCache.get("key3");  // 两级都未命中
        twoLevelCache.get("key2");  // L1命中

        System.out.println("统计信息:");
        twoLevelCache.printStats();

        // 验证统计
        assertEquals(5, twoLevelCache.getTotalReadCount());
        assertEquals(2, twoLevelCache.getL1HitCount());   // key1第二次，key2第二次
        assertEquals(2, twoLevelCache.getL2HitCount());   // key1第一次，key2第一次
        assertEquals(0.4, twoLevelCache.getL1HitRate(), 0.001);      // 2/5
        assertEquals(0.8, twoLevelCache.getOverallHitRate(), 0.001); // 4/5
    }

    @Test
    void testPreload() {
        // 在L2中准备数据
        for (int i = 0; i < 5; i++) {
            l2Cache.put("key" + i, "value" + i);
        }

        // 预加载前3个key到L1
        twoLevelCache.preloadToL1(Arrays.asList("key0", "key1", "key2"));

        // 验证L1有这些数据
        assertNotNull(l1Cache.get("key0"));
        assertNotNull(l1Cache.get("key1"));
        assertNotNull(l1Cache.get("key2"));
        assertNull(l1Cache.get("key3"));  // 未预加载

        // 获取预加载的数据应该L1命中
        twoLevelCache.get("key0");
        assertEquals(1, twoLevelCache.getL1HitCount());
    }

    @Test
    void testDemoteToL2() {
        twoLevelCache.put("key1", "value1");

        // demote前，L1和L2都有数据
        assertNotNull(l1Cache.get("key1"));
        assertNotNull(l2Cache.get("key1"));

        // demote
        twoLevelCache.demoteToL2("key1");

        // demote后，L1没有，L2还有
        assertNull(l1Cache.get("key1"));
        assertNotNull(l2Cache.get("key1"));

        // 再次获取，应该从L2获取并回填L1
        twoLevelCache.get("key1");
        assertNotNull(l1Cache.get("key1"));
    }

    @Test
    void testNullConstructorArgs() {
        // 测试构造方法参数检查
        assertThrows(IllegalArgumentException.class, () -> {
            new TwoLevelCache<>(null, l2Cache);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new TwoLevelCache<>(l1Cache, null);
        });
    }
}
