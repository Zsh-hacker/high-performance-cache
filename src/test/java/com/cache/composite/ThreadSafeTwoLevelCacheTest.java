package com.cache.composite;

import com.zsh.cache.Cache;
import com.zsh.cache.ConcurrentCache;
import com.zsh.cache.LRUCache;
import com.zsh.cache.composite.ThreadSafeTwoLevelCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * ThreadSafeTwoLevelCache并发测试
 */
class ThreadSafeTwoLevelCacheTest {

    private ThreadSafeTwoLevelCache<String, String> cache;

    @BeforeEach
    void setUp() {
        // 使用线程安全的缓存作为底层
        Cache<String, String> l1 = new ConcurrentCache<>();
        Cache<String, String> l2 = new ConcurrentCache<>();
        cache = new ThreadSafeTwoLevelCache<>(l1, l2);
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        final AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;

                        // 写入
                        cache.put(key, value);

                        // 读取（应该能读到）
                        String retrieved = cache.get(key);
                        if (!value.equals(retrieved)) {
                            errors.incrementAndGet();
                            System.err.printf("数据不一致: key=%s, expected=%s, actual=%s%n",
                                    key, value, retrieved);
                        }

                        // 随机读取其他线程可能写入的数据
                        if (j % 10 == 0 && threadId > 0) {
                            int otherThread = threadId - 1;
                            int otherIndex = j / 2;
                            cache.get("key-" + otherThread + "-" + otherIndex);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // 验证没有数据不一致
        assertEquals(0, errors.get(), "并发测试中发现错误");

        // 打印统计
        cache.printStats();

        System.out.println("并发测试完成，总操作数: " + (threadCount * operationsPerThread));
        System.out.println("L1命中率: " + (cache.getL1HitRate() * 100) + "%");
    }

    @Test
    void testConcurrentReadWrite() throws InterruptedException {
        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);

        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);

        // 先初始化一些数据
        for (int i = 0; i < 100; i++) {
            cache.put("init-" + i, "value-" + i);
        }

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    startLatch.await();  // 等待所有线程就绪

                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int j = 0; j < 500; j++) {
                        if (random.nextBoolean()) {
                            // 写操作
                            String key = "key-" + threadId + "-" + random.nextInt(1000);
                            cache.put(key, "value-" + System.currentTimeMillis());
                            writeCount.incrementAndGet();
                        } else {
                            // 读操作
                            String key = "key-" + random.nextInt(threadCount) + "-" + random.nextInt(100);
                            cache.get(key);
                            readCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 同时开始所有线程
        startLatch.countDown();

        // 等待所有线程完成
        endLatch.await();
        executor.shutdown();

        System.out.println("并发读写测试完成");
        System.out.println("读操作数: " + readCount.get());
        System.out.println("写操作数: " + writeCount.get());
        cache.printStats();
    }

    @Test
    void testConcurrentLoader() throws InterruptedException {
        // 测试带加载器的并发访问
        final AtomicInteger loaderCallCount = new AtomicInteger(0);

        Function<String, String> loader = key -> {
            loaderCallCount.incrementAndGet();
            try {
                Thread.sleep(10);  // 模拟慢加载
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "loaded-" + key;
        };

        Cache<String, String> l1 = new ConcurrentCache<>();
        Cache<String, String> l2 = new ConcurrentCache<>();
        ThreadSafeTwoLevelCache<String, String> loaderCache =
                new ThreadSafeTwoLevelCache<>(l1, l2, loader);

        final int threadCount = 5;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        // 所有线程同时请求同一个key
        final String hotKey = "hotKey";

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    String value = loaderCache.get(hotKey);
                    assertEquals("loaded-" + hotKey, value);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 加载器应该只被调用一次（由于锁保护）
        assertEquals(1, loaderCallCount.get(), "加载器应该只被调用一次");

        System.out.println("并发加载测试完成，加载器调用次数: " + loaderCallCount.get());
    }

    @Test
    void testSegmentLocks() throws InterruptedException {
        // 测试不同分段锁的并发性
        final int segmentCount = 4;
        Cache<String, String> l1 = new ConcurrentCache<>();
        Cache<String, String> l2 = new ConcurrentCache<>();
        ThreadSafeTwoLevelCache<String, String> segmentedCache =
                new ThreadSafeTwoLevelCache<>(l1, l2, null, segmentCount);

        final int threadCount = 8;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    // 每个线程操作不同的key集合（可能落在不同分段）
                    for (int j = 0; j < 1000; j++) {
                        String key = "key-" + (threadId * 100 + j);  // 确保不同线程的key不同
                        segmentedCache.put(key, "value");
                        segmentedCache.get(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("分段锁测试完成，耗时: %dms，分段数: %d%n", duration, segmentCount);
        segmentedCache.printStats();
    }
}