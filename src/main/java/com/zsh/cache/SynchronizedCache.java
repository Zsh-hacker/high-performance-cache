package com.zsh.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用synchronized实现线程安全的花村
 * 优点：
 * 简单易用、JVM自动管理锁的获取和释放、支持锁重入
 *
 * 缺点：
 * 性能较差（重量级锁）、不可中断、非公平锁、只有一个条件队列
 *
 * @param <K>
 * @param <V>
 */
public class SynchronizedCache<K, V> implements Cache<K, V>{
    private final Map<K, V> cache;

    /**
     * 默认构造方法
     */
    public SynchronizedCache() {
        this.cache = new HashMap<>();
    }

    /**
     * 指定初始容量的构造方法
     * @param initialCapacity
     */
    public SynchronizedCache(int initialCapacity) {
        this.cache = new HashMap<>(initialCapacity);
    }

    /**
     * 同步方法，锁对象this（当前实例）
     * @param key
     * @param value
     */
    @Override
    public synchronized void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public synchronized V get(K key) {
        return cache.get(key);
    }

    @Override
    public synchronized void remove(K key) {
        cache.remove(key);
    }

    @Override
    public synchronized void clear() {
        cache.clear();
    }

    @Override
    public synchronized int size() {
        return cache.size();
    }
}
