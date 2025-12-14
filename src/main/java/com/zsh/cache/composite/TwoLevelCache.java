package com.zsh.cache.composite;

import com.zsh.cache.Cache;

import java.util.function.Function;

/**
 * 二级缓存实现（组合模式）
 * L1：快速但容量小（如Caffeine、Guava Cache）
 * L2：较慢但容量大（如Redis、数据库）
 *
 * 读策略：L1 -> L2 -> 加载器
 * 写策略：Wrote-Through（同时写入两级）
 *
 * @param <K>
 * @param <V>
 */
public class TwoLevelCache<K, V> implements Cache<K, V> {

    private final Cache<K, V> level1;   // 一级缓存（快速、小容量）
    private final Cache<K, V> level2;   // 二级缓存（较慢、大容量）
    private final Function<K, V> loader;    // 数据加载器（当两级都未命中时使用）

    private long l1Hits = 0;
    private long l2Hits = 0;
    private long loaderCalls = 0;
    private long totalReads = 0;

    public TwoLevelCache(Cache<K, V> level1, Cache<K, V> level2) {
        this(level1, level2, null);
    }

    /**
     * 完整构造方法
     * @param level1    一级缓存
     * @param level2    二级缓存
     * @param loader    数据加载器，当缓存未命中时调用
     */
    public TwoLevelCache(Cache<K, V> level1, Cache<K, V> level2, Function<K, V> loader) {
        if (level1 == null || level2 == null) {
            throw new IllegalArgumentException("两级缓存都不能为null");
        }

        this.level1 = level1;
        this.level2 = level2;
        this.loader = loader;
    }

    @Override
    public void put(K key, V value) {
        level1.put(key, value);
        level2.put(key, value);
    }

    @Override
    public V get(K key) {
        totalReads++;

        V value = level1.get(key);
        if (value != null) {
            l1Hits++;
            return value;
        }

        value = level2.get(key);
        if (value != null) {
            l2Hits++;

            level1.put(key, value);
            return value;
        }

        if (loader != null) {
            loaderCalls++;
            value = loader.apply(key);
            if (value != null) {
                level1.put(key, value);
                level2.put(key, value);
            }
            return value;
        }
        // 没有加载器返回null
        return null;
    }

    @Override
    public void remove(K key) {
        level1.remove(key);
        level2.remove(key);
    }

    @Override
    public void clear() {
        level1.clear();
        level2.clear();
    }

    @Override
    public int size() {
        return level2.size();
    }

    public long getL1HitCount() {
        return l1Hits;
    }

    public long getL2HitCount() {
        return l2Hits;
    }

    public long getLoaderCallCount() {
        return loaderCalls;
    }

    public long getTotalReadCount() {
        return totalReads;
    }

    /**
     * 计算L1命中率
     */
    public double getL1HitRate() {
        return totalReads == 0 ? 0.0 : (double) l1Hits / totalReads;
    }

    /**
     * 计算总体命中率（L1+L2）
     */
    public double getOverallHitRate() {
        long hits = l1Hits + l2Hits;
        return totalReads == 0 ? 0.0 : (double) hits / totalReads;
    }

    /**
     * 获取统计信息字符串
     */
    public String getStatsString() {
        return String.format(
                "TwoLevelCache Stats: totalReads=%d, L1Hits=%d (%.1f%%), " +
                        "L2Hits=%d (%.1f%%), overallHitRate=%.1f%%, loaderCalls=%d",
                totalReads,
                l1Hits, getL1HitRate() * 100,
                l2Hits, (double) l2Hits / totalReads * 100,
                getOverallHitRate() * 100,
                loaderCalls
        );
    }

    /**
     * 打印统计信息
     */
    public void printStats() {
        System.out.println(getStatsString());
    }

    // ========== 高级功能 ==========

    /**
     * 预加载数据到L1缓存
     *
     * @param keys 要预加载的键
     */
    public void preloadToL1(Iterable<K> keys) {
        for (K key : keys) {
            if (level1.get(key) == null) {
                V value = level2.get(key);
                if (value != null) {
                    level1.put(key, value);
                }
            }
        }
    }

    /**
     * 将L1缓存中的热点数据同步到L2
     */
    public void syncHotDataToL2() {
        // 实际实现中需要知道L1的所有键
        // 这里简化为：如果L1是某种可遍历的缓存，可以同步
        // 由于Cache接口没有提供获取所有键的方法，这里留空
        System.out.println("同步热点数据到L2（需要具体实现支持）");
    }

    /**
     * 手动将数据降级到L2（从L1移除，保留在L2）
     */
    public void demoteToL2(K key) {
        V value = level1.get(key);
        if (value != null) {
            level2.put(key, value);  // 确保L2有最新值
            level1.remove(key);      // 从L1移除
        }
    }
}
