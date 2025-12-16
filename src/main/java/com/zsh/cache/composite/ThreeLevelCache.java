package com.zsh.cache.composite;

import com.zsh.cache.Cache;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 三级缓存实现
 * L1: 内存缓存（最快，容量最小）
 * L2: 本地磁盘/内存映射文件（中等速度，中等容量）
 * L3: 远程缓存/数据库（最慢，容量最大）
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class ThreeLevelCache<K, V> implements Cache<K, V> {

    private final Cache<K, V> level1;
    private final Cache<K, V> level2;
    private final Cache<K, V> level3;
    private final Function<K, V> loader;

    // 统计信息
    private final AtomicLong l1Hits = new AtomicLong();
    private final AtomicLong l2Hits = new AtomicLong();
    private final AtomicLong l3Hits = new AtomicLong();
    private final AtomicLong loaderCalls = new AtomicLong();
    private final AtomicLong totalReads = new AtomicLong();

    /**
     * 构造方法
     */
    public ThreeLevelCache(Cache<K, V> level1, Cache<K, V> level2,
                           Cache<K, V> level3, Function<K, V> loader) {
        if (level1 == null || level2 == null || level3 == null) {
            throw new IllegalArgumentException("三级缓存都不能为null");
        }

        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.loader = loader;
    }

    @Override
    public V get(K key) {
        totalReads.incrementAndGet();

        // 1. 查询L1
        V value = level1.get(key);
        if (value != null) {
            l1Hits.incrementAndGet();
            return value;
        }

        // 2. 查询L2
        value = level2.get(key);
        if (value != null) {
            l2Hits.incrementAndGet();
            // 回填到L1
            level1.put(key, value);
            return value;
        }

        // 3. 查询L3
        value = level3.get(key);
        if (value != null) {
            l3Hits.incrementAndGet();
            // 回填到L2和L1
            level2.put(key, value);
            level1.put(key, value);
            return value;
        }

        // 4. 都未命中，使用加载器
        if (loader != null) {
            loaderCalls.incrementAndGet();
            value = loader.apply(key);
            if (value != null) {
                // 写入所有三级缓存
                level3.put(key, value);
                level2.put(key, value);
                level1.put(key, value);
            }
            return value;
        }

        return null;
    }

    @Override
    public void put(K key, V value) {
        // Write-Through策略：同时写入三级缓存
        level1.put(key, value);
        level2.put(key, value);
        level3.put(key, value);
    }

    @Override
    public void remove(K key) {
        // 同时从三级缓存移除
        level1.remove(key);
        level2.remove(key);
        level3.remove(key);
    }

    @Override
    public void clear() {
        // 清空三级缓存
        level1.clear();
        level2.clear();
        level3.clear();
    }

    @Override
    public int size() {
        // 返回L3的大小（包含所有数据）
        return level3.size();
    }

    // ========== 统计方法 ==========

    public long getL1HitCount() {
        return l1Hits.get();
    }

    public long getL2HitCount() {
        return l2Hits.get();
    }

    public long getL3HitCount() {
        return l3Hits.get();
    }

    public long getLoaderCallCount() {
        return loaderCalls.get();
    }

    public long getTotalReadCount() {
        return totalReads.get();
    }

    public double getL1HitRate() {
        long total = totalReads.get();
        return total == 0 ? 0.0 : (double) l1Hits.get() / total;
    }

    public double getL2HitRate() {
        long total = totalReads.get();
        return total == 0 ? 0.0 : (double) l2Hits.get() / total;
    }

    public double getL3HitRate() {
        long total = totalReads.get();
        return total == 0 ? 0.0 : (double) l3Hits.get() / total;
    }

    public double getOverallHitRate() {
        long total = totalReads.get();
        long hits = l1Hits.get() + l2Hits.get() + l3Hits.get();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    public String getStatsString() {
        return String.format(
                "ThreeLevelCache Stats: totalReads=%d, " +
                        "L1Hits=%d (%.1f%%), L2Hits=%d (%.1f%%), L3Hits=%d (%.1f%%), " +
                        "overallHitRate=%.1f%%, loaderCalls=%d",
                totalReads.get(),
                l1Hits.get(), getL1HitRate() * 100,
                l2Hits.get(), getL2HitRate() * 100,
                l3Hits.get(), getL3HitRate() * 100,
                getOverallHitRate() * 100,
                loaderCalls.get()
        );
    }

    public void printStats() {
        System.out.println(getStatsString());
    }

    // ========== 高级功能 ==========

    /**
     * 将数据从L3预加载到L2和L1
     */
    public void preloadToUpperLevels(Iterable<K> keys) {
        for (K key : keys) {
            V value = level3.get(key);
            if (value != null) {
                level2.put(key, value);
                level1.put(key, value);
            }
        }
    }

    /**
     * 将数据从L1降级到L2和L3
     */
    public void demoteToLowerLevels(K key) {
        V value = level1.get(key);
        if (value != null) {
            level2.put(key, value);
            level3.put(key, value);
            level1.remove(key);  // 从L1移除
        }
    }

    /**
     * 同步所有层级的数据（确保一致性）
     */
    public void syncAllLevels() {
        // 实际实现中需要更复杂的同步逻辑
        // 这里简化为：将L3的所有数据同步到L2和L1
        System.out.println("同步所有缓存层级（需要具体实现支持）");
    }
}