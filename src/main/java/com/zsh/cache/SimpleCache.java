package com.zsh.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单缓存实现 - 基于HashMap
 * 线程不安全的实现
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class SimpleCache<K, V> implements Cache<K, V> {
    private final Map<K, V> cache;

    /**
     * 默认构造方法 - 使用HashMap的默认参数
     */
    public SimpleCache() {
        this.cache = new HashMap<>();
    }

    /**
     * 指定初始容量的构造方法
     * @param initialCapacity
     */
    public SimpleCache(int initialCapacity) {
        this.cache = new HashMap<>();
    }

    @Override
    public void put(K key, V value) {
        if(key == null) {
            throw new NullPointerException("键不能为null");
        }
        if(value == null) {
            throw new NullPointerException("值不能为null");
        }

        cache.put(key, value);
    }

    @Override
    public V get(K key) {
        if(key == null) {
            throw new NullPointerException("键不能为null");
        }

        return cache.get(key);
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
     * 判断缓存是否为空
     * @return
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    /**
     * 判断是否包含指定键
     * @param key
     * @return
     */
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }
}
