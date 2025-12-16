package com.zsh.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * 优化的并发缓存实现
 * 1. 使用LongAdder代替AtomicLong进行统计（减少竞争）
 * 2. 预计算hashCode，减少重复计算
 * 3. 使用computeIfAbsent原子操作
 * 4. 避免不必要的对象创建
 * @param <K>
 * @param <V>
 */
public class OptimizedConcurrentCache<K, V> implements Cache<K, V> {

    /**
     * 缓存项，包装原始值
     * 可以在这里添加版本号、访问时间等元数据
     */
    static class CacheEntry<V> {
        final V value;
        final int hashCode;

        CacheEntry(V value) {
            this.value = value;
            this.hashCode = value != null ? value.hashCode() : 0;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            CacheEntry<?> that = (CacheEntry<?>) obj;
            return value != null ? value.equals(that.value) : that.value == null;
        }
    }

    private final ConcurrentMap<K, CacheEntry<V>> cache;
    private final Function<K, V> loader;

    // 使用LongAddr进行统计（高并发下性能更好）
    private final LongAdder hitCount = new LongAdder();
    private final LongAdder missCount = new LongAdder();
    private final LongAdder loadCount = new LongAdder();

    /**
     * 默认构造方法
     */
    public OptimizedConcurrentCache() {
        this(null);
    }

    /**
     * 带加载器的构造方法
     */
    public OptimizedConcurrentCache(Function<K, V> loader) {
        this(16, 0.75f, 16, loader);
    }

    /**
     * 完整构造方法
     */
    public OptimizedConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel, Function<K, V> loader) {
        this.cache = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
        this.loader = loader;
    }

    @Override
    public V get(K key) {
        // 尝试获取现有值
        CacheEntry<V> entry = cache.get(key);

        if (entry != null) {
            hitCount.increment();
            return entry.value;
        }
        missCount.increment();
        if(loader != null) {
            return loadValue(key);
        }

        return null;
    }

    /**
     * 加载值（线程安全）
     */
    private V loadValue(K key) {
        // 使用computeIfAbsent确保原子性
        CacheEntry<V> entry = cache.computeIfAbsent(key, k -> {
            loadCount.increment();
            V value = loader.apply(k);
            return value != null ? new CacheEntry<>(value) : null;
        });
        return entry != null ? entry.value : null;
    }

    @Override
    public void put(K key, V value) {
        CacheEntry<V> entry = value != null ? new CacheEntry<>(value) : null;
        cache.put(key, entry);
    }

    /**
     * 优化的put方法：如果key不存在才放入
     * 返回旧值（如果存在）
     */
    public V putIfAbsent(K key, V value) {
        CacheEntry<V> oldEntry = cache.putIfAbsent(key,
                value != null ? new CacheEntry<>(value) : null);
        return oldEntry != null ? oldEntry.value : null;
    }

    /**
     * 使用函数计算值并放入（原子操作）
     */
    public V compute(K key, Function<? super  K, ? extends V> remappingFunction) {
        CacheEntry<V> entry = cache.compute(key, (k, oldEntry) -> {
            V newValue = remappingFunction.apply(k);
            return newValue != null ? new CacheEntry<>(newValue) : null;
        });
        return entry != null ? entry.value : null;
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    /**
     * 条件移除：只有值与expect先相同时才移除
     */
    public boolean remove(K key, V expect) {
        if (expect == null) {
            return cache.remove(key) != null;
        }
        return cache.remove(key, new CacheEntry<>(expect));
    }

    @Override
    public void clear() {
        cache.clear();
        // 重置统计
        // resetStats();
    }

    @Override
    public int size() {
        return cache.size();
    }

    // ========== 统计方法 ==========

    public long getHitCount() {
        return hitCount.sum();
    }

    public long getMissCount() {
        return missCount.sum();
    }

    public long getLoadCount() {
        return loadCount.sum();
    }

    public double getHitRate() {
        long hits = hitCount.sum();
        long misses = missCount.sum();
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }

    public void resetStats() {
        hitCount.reset();
        missCount.reset();
        loadCount.reset();
    }

    /**
     * 获取缓存内部统计信息（用于调试）
     */
    public String getInternalStats() {
        if (cache instanceof ConcurrentHashMap) {
            ConcurrentHashMap<K, CacheEntry<V>> map = (ConcurrentHashMap<K, CacheEntry<V>>) cache;
            // 注意：这些方法是JDK内部的，可能在不同版本间变化
            // 实际生产代码中应该谨慎使用
            try {
                // 尝试获取一些内部信息
                return String.format("size=%d, estimatedSize=%d",
                        map.size(), map.mappingCount());
            } catch (Exception e) {
                // 如果方法不存在或不可访问，返回基本信息
                return "size=" + map.size();
            }
        }
        return "size=" + cache.size();
    }

    /**
     * 预热缓存：预先加载一批数据
     */
    public void warmUp(Iterable<? extends K> keys) {
        if (loader == null) {
            throw new IllegalStateException("没有加载器，无法预热");
        }

        for (K key : keys) {
            cache.computeIfAbsent(key, k -> {
                V value = loader.apply(k);
                return value != null ? new CacheEntry<>(value) : null;
            });
        }
    }
}
