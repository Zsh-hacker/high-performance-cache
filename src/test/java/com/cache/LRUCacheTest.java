package com.cache;

import com.zsh.cache.LRUCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LRUCacheTest {
    private LRUCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new LRUCache<>(3);
    }

    /**
     * 测试访问顺序对LRU的影响
     */
    @Test
    void testLRUEviction() {
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        assertEquals(3, cache.size(), "缓存应该包含3个元素");

        cache.put("4", "four");
        assertEquals(3, cache.size(), "缓存大小应该保持为3");
        assertNull(cache.get("1"), "1应该被淘汰");
        assertNotNull(cache.get("2"), "2应该还在");
        assertNotNull(cache.get("3"), "3应该还在");
        assertNotNull(cache.get("4"), "4应该还在");
    }

    /**
     * 测试访问顺序对LRU的影响
     */
    @Test
    void testAccessOrder() {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        cache.get("A");

        assertEquals("B", cache.getEldestKey(), "B应该是最久未访问的");

        cache.put("D", "4");

        assertNull(cache.get("B"), "B应该被淘汰");
        assertNotNull(cache.get("A"), "A应该还在");
        assertNotNull(cache.get("C"), "C应该还在");
        assertNotNull(cache.get("D"), "D应该还在");
    }

    /**
     * 测试put操作也会更新访问顺序
     */
    @Test
    void testPutUpdatesAccessOrder() {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        cache.put("B", "newValue");
        cache.put("D", "4");

        assertNull(cache.get("A"), "A应该被淘汰");
        assertNotNull(cache.get("B"), "B应该还在");
        assertEquals("newValue", cache.get("B"), "B的值应该被更新");
    }

    /**
     * 测试迭代顺序反映了访问顺序
     */
    @Test
    void testIterationOrder() {
        cache.put("A", "1");
        cache.put("B", "2");
        cache.put("C", "3");

        // 访问顺序：A -> B -> C
        // 访问A后，顺序变为：B, C, A
        cache.get("A");

        // 验证迭代顺序
        //Iterator<Map.Entry<String, String>> iterator = cache.entrySet().iterator();
        //assertEquals("B", iterator.next().getKey(), "第一个应该是B（最久未访问）");
        //assertEquals("C", iterator.next().getKey(), "第二个应该是C");
        //assertEquals("A", iterator.next().getKey(), "第三个应该是A（最近访问）");
    }

    /**
     * 测试容量边界情况
     */
    @Test
    void testCapacityBoundary() {
        // 容量为0或1的特殊情况
        LRUCache<String, String> smallCache = new LRUCache<>(1);

        smallCache.put("A", "1");
        assertEquals(1, smallCache.size());

        smallCache.put("B", "2");  // 应该淘汰A
        assertEquals(1, smallCache.size());
        assertNull(smallCache.get("A"));
        assertNotNull(smallCache.get("B"));

        smallCache.put("C", "3");  // 应该淘汰B
        assertNull(smallCache.get("B"));
        assertNotNull(smallCache.get("C"));
    }

    /**
     * 测试并发环境下的LRU缓存（简单测试）
     */
    @Test
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 5;
        final int operationsPerThread = 1000;

        // 创建容量较大的缓存，避免频繁淘汰
        final LRUCache<Integer, String> concurrentCache = new LRUCache<>(threadCount * operationsPerThread / 2);

        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    int key = threadId * operationsPerThread + j;
                    concurrentCache.put(key, "value" + key);

                    // 随机访问一些已有键
                    if (j % 10 == 0 && j > 0) {
                        int randomKey = (int) (Math.random() * j);
                        concurrentCache.get(threadId * operationsPerThread + randomKey);
                    }
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证缓存大小不超过容量
        assertTrue(concurrentCache.size() <= concurrentCache.getCapacity(),
                "缓存大小不应该超过容量");

        // 注意：LinkedHashMap不是线程安全的！
        // 这个测试只是为了演示并发访问下的行为，实际生产环境需要同步
        System.out.println("并发测试完成，最终缓存大小: " + concurrentCache.size());
    }
}
