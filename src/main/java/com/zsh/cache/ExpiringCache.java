package com.zsh.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 支持过期时间的缓存
 *
 * 实现方案：
 * 1. 使用ConcurrentHashMap存储，支持高并发
 * 2. 每个缓存项记录过期时间
 * 3. 惰性删除：get时检查是否过期
 * 4. 定时清理：后台线程定期清理过期缓存
 *
 * @param <K>
 * @param <V>
 */
public class ExpiringCache<K,V> implements Cache<K,V>{

    /**
     * 缓存项，包含值和过期时间
     * @param <V>
     */
    static class CacheEntry<V> {
        final V value;
        final long expireTime;

        CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        /**
         * 检查是否过去
         * @return
         */
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final ScheduledExecutorService cleaner;

    // 默认过期时间（毫秒）
    private final long defaultExpireTime;

    /**
     * 默认构造方法
     * @param defaultExpireTime
     */
    public ExpiringCache(long defaultExpireTime) {
        this(defaultExpireTime, 60, TimeUnit.SECONDS);   // 默认60秒清理一次
    }

    /**
     * 完整构造方法
     * @param defaultExpireTime
     * @param cleanupInterval
     * @param unit
     */
    public ExpiringCache(long defaultExpireTime, long cleanupInterval, TimeUnit unit) {
        if (defaultExpireTime <= 0) {
            throw new IllegalArgumentException("默认过期时间必须大于0");
        }
        this.defaultExpireTime = defaultExpireTime;
        this.cache = new ConcurrentHashMap<>();

        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ExpiringCache-Cleaner");
            thread.setDaemon(true);
            return thread;
        });
        // 启动定时清理任务
        this.cleaner.scheduleAtFixedRate(this::cleanupExpiredEntries,
                cleanupInterval, cleanupInterval, unit);
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultExpireTime);
    }

    /**
     * 放入缓存项，指定过期时间
     * @param key
     * @param value
     * @param expireTime 过期时间（毫秒）
     */
    public void put(K key, V value, long expireTime) {
        if (expireTime <= 0) {
            throw new IllegalArgumentException("过期时间必须大于0");
        }

        long expireTimestamp = System.currentTimeMillis() + expireTime;
        CacheEntry<V> entry = new CacheEntry<>(value, expireTimestamp);
        cache.put(key, entry);
    }

    @Override
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;    // 缓存未命中
        }
        // 惰性删除：检查是否过期
        if(entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        return cache.size();
    }

    /**
     * 获取有效缓存数量
     * @return
     */
    public int validSize() {
        int count = 0;
        for (CacheEntry<V> entry : cache.values()) {
            if (!entry.isExpired()) {
                count ++;
            }
        }
        return count;
    }

    /**
     * 定时清理任务：移除所有过期数据
     */
    private void cleanupExpiredEntries() {
        int before = cache.size();

        // 遍历所有缓存项，移除过期的
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        int after = cache.size();
        int removed = before - after;
        if (removed > 0) {
            System.out.println("清理了 " + removed + " 个过期缓存项");
        }
    }

    /**
     * 关闭缓存，停止清理任务
     * 注意：关闭后缓存将不再自动清理过期缓存
     */
    public void shutdown() {
        cleaner.shutdown();
        try {
            if (!cleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleaner.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取默认过期时间
     * @return
     */
    public long getDefaultExpireTime() {
        return defaultExpireTime;
    }

    /**
     * 获取缓存项（不检查过期）
     * @param key
     * @return
     */
    CacheEntry<V> getEntry(K key) {
        return cache.get(key);
    }

}
