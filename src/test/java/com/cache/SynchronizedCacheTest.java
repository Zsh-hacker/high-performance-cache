package com.cache;

import com.zsh.cache.SimpleCache;
import com.zsh.cache.SynchronizedCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SynchronizedCacheTest {
    private SynchronizedCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new SynchronizedCache<>();
    }

    @Test
    void testConcurrentPut() throws InterruptedException {
        final SynchronizedCache<String, Integer> cache = new SynchronizedCache<>();
        final int threadCount = 10;
        final int putCount = 1000;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.execute(() -> {
                try {
                    for (int j = 0; j < putCount; j++) {
                        String key = "key" + threadId + "_" + j;
                        cache.put(key, threadId*1000+j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        int expectedSize = threadCount * putCount;
        int actualSize = cache.size();
        System.out.println("预期大小:" + expectedSize);
        System.out.println("实际大小:" + actualSize);
        System.out.println("丢失数据:" + (expectedSize - actualSize));

        // 验证: 至少检查一些数据是否存在
        AtomicInteger foundCount = new AtomicInteger(0);
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < putCount; j++) {
                String key = "key" + i + "_" + j;
                if(cache.get(key) != null) {
                    foundCount.incrementAndGet();
                }
            }
        }
        System.out.println("找到的数据数量:" + foundCount.get());
        assertTrue(foundCount.get() > 0, "应该至少能找到一些数据");
    }

    @Test
    void testConcurrentGetAndPut() throws InterruptedException {
        final SynchronizedCache<String, Integer> cache = new SynchronizedCache<>();
        final String testKey = "testKey";
        final int threadCount = 10;

        cache.put(testKey, 0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Integer current = cache.get(testKey);
                        if (current != null) {
                            cache.put(testKey, current + 1);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        // 理论上最终值应该是threadCount * 100
        Integer finalValue = cache.get(testKey);
        System.out.println("最终值：" + finalValue);
        System.out.println("期望值：" + (threadCount * 100));
    }

    @Test
    void testConcurrentGetAndPut1() throws InterruptedException {
        final SynchronizedCache<String, Integer> cache = new SynchronizedCache<>();
        final String testKey = "testKey";
        final int threadCount = 10;

        // 先放入初始值
        cache.put(testKey, 0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 每个线程执行100次：读取值、加1、写回
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        Integer current = cache.get(testKey);
                        if (current != null) {
                            cache.put(testKey, current + 1);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 理论上最终值应该是 threadCount * 100
        Integer finalValue = cache.get(testKey);
        System.out.println("最终值: " + finalValue);
        System.out.println("期望值: " + (threadCount * 100));

        // 由于没有同步，最终值可能小于期望值
        // 这是因为get和put不是原子操作，存在竞态条件
    }
}
