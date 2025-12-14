package com.cache;

import com.zsh.cache.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 不同缓存实现的性能对比测试
 */
class CachePerformanceTest {

    /**
     * 测试并发写入性能
     */
    @Test
    void testWritePerformance() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 10000;

        // 测试不同的缓存实现
        testCacheWrite(new SimpleCache<>(), "SimpleCache", threadCount, operationsPerThread);
        testCacheWrite(new SynchronizedCache<>(), "SynchronizedCache", threadCount, operationsPerThread);
        testCacheWrite(new ReenTrantLockCache<>(), "ReentrantLockCache", threadCount, operationsPerThread);
        testCacheWrite(new ConcurrentCache<>(), "ConcurrentCache", threadCount, operationsPerThread);
    }

    private void testCacheWrite(Cache<String, Integer> cache, String cacheName,
                                int threadCount, int operationsPerThread) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        cache.put(key, threadId * operationsPerThread + j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.printf("%s - 写入性能测试:\n", cacheName);
        System.out.printf("  线程数: %d, 每个线程操作数: %d\n", threadCount, operationsPerThread);
        System.out.printf("  总操作数: %d\n", threadCount * operationsPerThread);
        System.out.printf("  总耗时: %.2f ms\n", duration / 1_000_000.0);
        System.out.printf("  吞吐量: %.2f ops/ms\n",
                (threadCount * operationsPerThread) / (duration / 1_000_000.0));
        System.out.printf("  最终缓存大小: %d\n", cache.size());
        System.out.println();
    }

    /**
     * 测试并发读写混合性能
     */
    @Test
    void testReadWritePerformance() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 10000;
        float writeRatio = 0.3f;  // 30%写操作，70%读操作

        System.out.println("=== 读写混合性能测试 ===");
        System.out.printf("写操作比例: %.1f%%\n", writeRatio * 100);

        testCacheReadWrite(new SimpleCache<>(), "SimpleCache", threadCount, operationsPerThread, writeRatio);
        testCacheReadWrite(new ReadWriteLockCache<>(), "ReadWriteLockCache", threadCount, operationsPerThread, writeRatio);
        testCacheReadWrite(new SynchronizedCache<>(), "SynchronizedCache", threadCount, operationsPerThread, writeRatio);
        testCacheReadWrite(new ReenTrantLockCache<>(), "ReentrantLockCache", threadCount, operationsPerThread, writeRatio);
        testCacheReadWrite(new ConcurrentCache<>(), "ConcurrentCache", threadCount, operationsPerThread, writeRatio);
    }

    private void testCacheReadWrite(Cache<String, Integer> cache, String cacheName,
                                    int threadCount, int operationsPerThread, float writeRatio)
            throws InterruptedException {

        // 先预热，放入一些数据
        for (int i = 0; i < 1000; i++) {
            cache.put("preheat-" + i, i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (random.nextFloat() < writeRatio) {
                            // 写操作
                            String key = "key-" + threadId + "-" + random.nextInt(1000);
                            cache.put(key, random.nextInt());
                            writeCount.incrementAndGet();
                        } else {
                            // 读操作
                            String key = "key-" + random.nextInt(threadCount) + "-" + random.nextInt(1000);
                            cache.get(key);
                            readCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.printf("\n%s:\n", cacheName);
        System.out.printf("  读操作: %d, 写操作: %d\n", readCount.get(), writeCount.get());
        System.out.printf("  总耗时: %.2f ms\n", duration / 1_000_000.0);
        System.out.printf("  总吞吐量: %.2f ops/ms\n",
                (readCount.get() + writeCount.get()) / (duration / 1_000_000.0));
    }

    /**
     * 测试竞争激烈情况下的性能
     */
    @Test
    void testHighContention() throws InterruptedException {
        int threadCount = 20;
        int operationsPerThread = 5000;

        // 使用少量热点key，模拟高竞争场景
        String[] hotKeys = {"hotKey1", "hotKey2", "hotKey3", "hotKey4", "hotKey5"};

        System.out.println("=== 高竞争场景测试 ===");
        System.out.println("热点key数量: " + hotKeys.length);

        testHighContention(new SynchronizedCache<>(), "SynchronizedCache",
                threadCount, operationsPerThread, hotKeys);
        testHighContention(new ReenTrantLockCache<>(true), "ReentrantLockCache(公平锁)",
                threadCount, operationsPerThread, hotKeys);
        testHighContention(new ReenTrantLockCache<>(false), "ReentrantLockCache(非公平锁)",
                threadCount, operationsPerThread, hotKeys);
        testHighContention(new ConcurrentCache<>(), "ConcurrentCache",
                threadCount, operationsPerThread, hotKeys);
    }

    private void testHighContention(Cache<String, Integer> cache, String cacheName,
                                    int threadCount, int operationsPerThread, String[] hotKeys)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // 80%的操作集中在热点key上
                        String key;
                        if (random.nextFloat() < 0.8f) {
                            key = hotKeys[random.nextInt(hotKeys.length)];
                        } else {
                            key = "normal-" + threadId + "-" + random.nextInt(100);
                        }

                        if (random.nextBoolean()) {
                            cache.put(key, random.nextInt());
                        } else {
                            cache.get(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        System.out.printf("\n%s:\n", cacheName);
        System.out.printf("  总耗时: %.2f ms\n", duration / 1_000_000.0);
        System.out.printf("  吞吐量: %.2f ops/ms\n",
                (threadCount * operationsPerThread) / (duration / 1_000_000.0));
    }
}