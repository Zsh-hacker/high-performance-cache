package com.zsh.cache.composite;

import com.zsh.cache.Cache;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * 线程安全策略：
 * 1. 统计变量使用Atomiclong
 * 2. 使用分段锁：对不同的key使用不同的锁，减少竞争
 * 3. 使用读写锁：读多写少的场景
 *
 * @param <K>
 * @param <V>
 */
public class ThreadSafeTwoLevelCache<K, V> implements Cache<K, V> {

    private final Cache<K, V> level1;
    private final Cache<K, V> level2;
    private final Function<K, V> loader;

    // 使用分段锁数组
    private final ReentrantLock[] segmentLocks;
    private static final int DEFAULT_SEGMENT_COUNT = 16;

    // 使用LongAddr的替代方案：AtomicLong
    private final AtomicLong l1Hits = new AtomicLong();
    private final AtomicLong l2Hits = new AtomicLong();
    private final AtomicLong loaderCalls = new AtomicLong();
    private final AtomicLong totalReads = new AtomicLong();

    /**
     * 构造方法
     */
    public ThreadSafeTwoLevelCache(Cache<K, V> level1, Cache<K, V> level2) {
        this(level1, level2, null, DEFAULT_SEGMENT_COUNT);
    }

    public ThreadSafeTwoLevelCache(Cache<K, V> level1, Cache<K, V> level2, Function<K, V> loader) {
        this(level1, level2, null, DEFAULT_SEGMENT_COUNT);
    }

    public ThreadSafeTwoLevelCache(Cache<K, V> level1, Cache<K, V> level2, Function<K, V> loader, int segmentCount) {
        if (level1 == null || level2 == null) {
            throw new IllegalArgumentException("两级缓存都不能为null");
        }
        if (segmentCount <= 0) {
            segmentCount = DEFAULT_SEGMENT_COUNT;
        }

        this.level1 = level1;
        this.level2 = level2;
        this.loader = loader;
        this.segmentLocks = new ReentrantLock[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segmentLocks[i] = new ReentrantLock();
        }
    }

    @Override
    public V get(K key) {
        totalReads.incrementAndGet();

        V value = level1.get(key);
        if (value != null) {
            l1Hits.incrementAndGet();
            return value;
        }

        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            // 双重检查锁：在获取锁后再次检查L1（可能其他线程已经回填）
            value = level1.get(key);
            if (value != null) {
                l1Hits.incrementAndGet();
                return value;
            }
            value = level2.get(key);
            if (value != null) {
                l2Hits.incrementAndGet();
                level1.put(key, value);
                return value;
            }
            // 两级都未命中，使用加载器
            if (loader != null) {
                loaderCalls.incrementAndGet();
                value = loader.apply(key);
                if (value != null) {
                    level1.put(key, value);
                    level2.put(key, value);
                }
                return value;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            level1.put(key, value);
            level2.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(K key) {
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            level1.remove(key);
            level2.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        for (ReentrantLock lock : segmentLocks) {
            lock.lock();
        }
        try {
            level1.clear();
            level2.clear();

            resetStats();
        } finally {
            for (ReentrantLock lock : segmentLocks) {
                lock.unlock();
            }
        }
    }

    @Override
    public int size() {
        return level2.size();
    }

    // ========== 私有方法 ==========
    /**
     * 根据key获取对应的分段锁
     */
    private ReentrantLock getLock(K key) {
        int hash = key != null ? key.hashCode() : 0;
        int index = Math.abs(hash) % segmentLocks.length;
        return segmentLocks[index];
    }

    // ========== 统计方法（线程安全） ==========

    public AtomicLong getL1HitCount() {
        return l1Hits;
    }

    public AtomicLong getL2HitCount() {
        return l2Hits;
    }

    public AtomicLong getLoaderCallCount() {
        return loaderCalls;
    }

    public AtomicLong getTotalReadCount() {
        return totalReads;
    }

    public double getL1HitRate() {
        long total = totalReads.get();
        return total == 0 ? 0.0 : (double) l1Hits.get() / total;
    }

    public double getOverallHitRate() {
        long total = totalReads.get();
        long hits = l1Hits.get() + l2Hits.get();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        l1Hits.set(0);
        l2Hits.set(0);
        loaderCalls.set(0);
        totalReads.set(0);
    }

    public String getStatsString() {
        return String.format(
                "ThreadSafeTwoLevelCache Stats: totalReads=%d, L1Hits=%d (%.1f%%), " +
                        "L2Hits=%d (%.1f%%), overallHitRate=%.1f%%, loaderCalls=%d, segments=%d",
                totalReads.get(),
                l1Hits.get(), getL1HitRate() * 100,
                l2Hits.get(), (double) l2Hits.get() / Math.max(1, totalReads.get()) * 100,
                getOverallHitRate() * 100,
                loaderCalls.get(),
                segmentLocks.length
        );
    }

    public void printStats() {
        System.out.println(getStatsString());
    }
}
