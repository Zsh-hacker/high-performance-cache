package com.zsh.cache.decorator;

import com.zsh.cache.Cache;

import java.util.concurrent.atomic.AtomicLong;

public class CacheWithStats<K,V> implements Cache<K,V> {
    private final Cache<K,V> delegate;

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong removeCount = new AtomicLong();
    private  final AtomicLong totalGetTime = new AtomicLong();  // 总获取时间（纳秒）
    private final AtomicLong totalPutTime = new AtomicLong();   // 总放入时间（纳秒）

    public CacheWithStats(Cache<K, V> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("被装饰的缓存不能为null");
        }
        this.delegate = delegate;
    }


    @Override
    public void put(K key, V value) {
        long startTime = System.nanoTime();
        try {
            delegate.put(key, value);
            putCount.incrementAndGet();
        } finally {
            long duration = System.nanoTime() - startTime;
            totalPutTime.addAndGet(duration);
        }
    }

    @Override
    public V get(K key) {
        long startTime = System.nanoTime();
        try {
            V value = delegate.get(key);
            if (value != null) {
                hitCount.incrementAndGet();
            } else {
                missCount.incrementAndGet();
            }
            return value;
        } finally {
            long duration = System.nanoTime() - startTime;
            totalGetTime.addAndGet(duration);
        }
    }

    @Override
    public void remove(K key) {
        delegate.remove(key);
        removeCount.incrementAndGet();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    // ========== 统计相关方法 ==========

    /**
     * 获取命中次数
     * @return
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * 获取未命中次数
     * @return
     */
    public long getMissCount() {
        return missCount.get();
    }

    /**
     * 获取总访问次数
     * @return
     */
    public long getTotalAccessCount() {
        return hitCount.get() + missCount.get();
    }

    /**
     * 计算命中率
     * @return
     */
    public double getHitRate() {
        long total = getTotalAccessCount();
        return total == 0 ? 0 : (double) getHitCount() / total;
    }

    /**
     * 获取平均获取时间（纳秒）
     * @return
     */
    public double getAverageGetTime() {
        long totalAccess = getTotalAccessCount();
        return totalAccess == 0 ? 0.0 : (double) totalGetTime.get() / totalAccess;
    }

    /**
     * 获取平均放入时间
     * @return
     */
    public double getAveragePutTime() {
        long puts = putCount.get();
        return puts == 0 ? 0.0 : (double) totalPutTime.get() / puts;
    }

    /**
     * 获取放入次数
     * @return
     */
    public long getPutCount() {
        return putCount.get();
    }

    /**
     * 获取移除次数
     * @return
     */
    public long getRemoveCount() {
        return removeCount.get();
    }

    /**
     * 重置所有统计
     */
    public void resetStats() {
        hitCount.set(0);
        missCount.set(0);
        putCount.set(0);
        removeCount.set(0);
        totalGetTime.set(0);
        totalPutTime.set(0);
    }

    /**
     * 获取统计信息字符串
     */
    public String getStatsString() {
        return String.format(
                "Cache Stats: hits=%d, misses=%d, hitRate=%.2f%%, " +
                        "avgGetTime=%.2fns, avgPutTime=%.2fns, puts=%d, removes=%d",
                getHitCount(), getMissCount(), getHitRate() * 100,
                getAverageGetTime(), getAveragePutTime(),
                getPutCount(), getRemoveCount()
        );
    }

    /**
     * 打印统计信息
     */
    public void printStats() {
        System.out.println(getStatsString());
    }
}
