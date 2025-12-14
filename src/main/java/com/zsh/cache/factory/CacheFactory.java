package com.zsh.cache.factory;

import com.zsh.cache.*;
import com.zsh.cache.composite.TwoLevelCache;
import com.zsh.cache.decorator.CacheWithStats;

import java.util.function.Function;

/**
 * 缓存工厂类
 * 使用工厂模式统一创建缓存对象
 */
public class CacheFactory {

    private CacheFactory() {
        // 私有构造方法，防止实例化
    }

    // ========== 基础缓存工厂方法 ==========

    /**
     * 创建简单缓存（非线程安全）
     * @return
     * @param <K>
     * @param <V>
     */
    public static <K, V> Cache<K, V> createSimpleCache() {
        return new SimpleCache<>();
    }

    public static <K, V> Cache<K, V> createSimpleCache(int initialCapacity) {
        return new SimpleCache<>(initialCapacity);
    }

    /**
     * 创建同步缓存（synchronized）
     */
    public static <K, V> Cache<K, V> createSynchronizedCache() {
        return new SynchronizedCache<>();
    }

    public static <K, V> Cache<K, V> createSynchronizedCache(int initialCapacity) {
        return new SynchronizedCache<>(initialCapacity);
    }

    /**
     * 创建ReentrantLock缓存
     */
    public static <K, V> Cache<K, V> createReentrantLockCache() {
        return new ReenTrantLockCache<>();
    }

    public static <K, V> Cache<K, V> createReentrantLockCache(boolean fair) {
        return new ReenTrantLockCache<>(fair);
    }

    /**
     * 创建并发缓存（ConcurrentHashMap）
     */
    public static <K, V> Cache<K, V> createConcurrentCache() {
        return new ConcurrentCache<>();
    }

    public static <K, V> Cache<K, V> createConcurrentCache(int initialCapacity) {
        return new ConcurrentCache<>(initialCapacity);
    }

    // ========== LRU缓存工厂方法 ==========
    /**
     * 创建LRU缓存
     */
    public static <K, V> Cache<K, V> createLRUCache(int capacity) {
        return new LRUCache<>(capacity);
    }

    /**
     * 创建线程安全的LRU缓存
     */
    public static <K, V> Cache<K, V> createSynchronizedLRUCache(int capacity) {
        LRUCache<K, V> lruCache = new LRUCache<>(capacity);
        return new SynchronizedCache<K, V>() {
            @Override
            public V get(K key) {
                synchronized (this) {
                    return lruCache.get(key);
                }
            }

            @Override
            public void put(K key, V value) {
                synchronized (this) {
                    lruCache.put(key, value);
                }
            }

            @Override
            public void remove(K key) {
                synchronized (this) {
                    lruCache.remove(key);
                }
            }

            @Override
            public void clear() {
                synchronized (this) {
                    lruCache.clear();
                }
            }

            @Override
            public int size() {
                synchronized (this) {
                    return lruCache.size();
                }
            }
        };
    }

    // ========== 装饰器缓存工厂方法 ==========
    /**
     * 创建带统计功能的缓存
     */
    public static <K, V> Cache<K, V> createCacheWithStats(Cache<K, V> delegate) {
        return new CacheWithStats<>(delegate);
    }

    /**
     * 创建带统计的LRU缓存
     */
    public static <K, V> Cache<K, V> createLRUCacheWithStats(int capacity) {
        Cache<K, V> lruCache = createLRUCache(capacity);
        return createCacheWithStats(lruCache);
    }

    /**
     * 创建带统计的并发缓存
     */
    public static <K, V> Cache<K, V> createConcurrentCacheWithStats(int initialCapacity) {
        Cache<K, V> concurrentCache = createConcurrentCache(initialCapacity);
        return createCacheWithStats(concurrentCache);
    }

    // ========== 组合缓存工厂方法 ==========

    /**
     * 创建二级缓存
     */
    public static <K, V> Cache<K, V> createTwoLevelCache(
            Cache<K, V> level1,
            Cache<K, V> level2) {
        return new TwoLevelCache<>(level1, level2);
    }

    /**
     * 创建带加载器的二级缓存
     */
    public static <K, V> Cache<K, V> createTwoLevelCache(
            Cache<K, V> level1,
            Cache<K, V> level2,
            Function<K, V> loader) {
        return new TwoLevelCache<>(level1, level2, loader);
    }

    /**
     * 创建推荐的缓存配置（生产环境）
     * L1: 带统计的LRU缓存（容量小）
     * L2: 并发缓存（容量大）
     */
    public static <K, V> Cache<K, V> createRecommendedCache(
            int l1Capacity,
            int l2InitialCapacity) {

        // L1: 带统计的LRU缓存
        Cache<K, V> l1Cache = createLRUCacheWithStats(l1Capacity);

        // L2: 并发缓存
        Cache<K, V> l2Cache = createConcurrentCache(l2InitialCapacity);

        // 组合成二级缓存
        return createTwoLevelCache(l1Cache, l2Cache);
    }

    /**
     * 根据配置字符串创建缓存
     * 支持配置: "simple", "sync", "concurrent", "lru:100", "lru-stats:100"
     */
    public static <K, V> Cache<K, V> createCacheFromConfig(String config) {
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException("配置不能为空");
        }

        String[] parts = config.split(":");
        String type = parts[0].trim().toLowerCase();

        switch (type) {
            case "simple":
                return createSimpleCache();

            case "sync":
                return createSynchronizedCache();

            case "concurrent":
                if (parts.length > 1) {
                    int capacity = Integer.parseInt(parts[1]);
                    return createConcurrentCache(capacity);
                }
                return createConcurrentCache();

            case "lru":
                if (parts.length < 2) {
                    throw new IllegalArgumentException("LRU缓存需要指定容量，如: lru:100");
                }
                int capacity = Integer.parseInt(parts[1]);
                return createLRUCache(capacity);

            case "lru-stats":
                if (parts.length < 2) {
                    throw new IllegalArgumentException("LRU统计缓存需要指定容量，如: lru-stats:100");
                }
                capacity = Integer.parseInt(parts[1]);
                return createLRUCacheWithStats(capacity);

            default:
                throw new IllegalArgumentException("不支持的缓存类型: " + type);
        }
    }
}
