package com.zsh.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentCache<K, V> implements Cache<K, V>{
    private final Map<K, V> cache;

    /**
     * 默认构造方法
     */
    public ConcurrentCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public ConcurrentCache(int initialCapacity) {
        this.cache = new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * 指定初始容量和负载因子的构造方法
     * @param initialCapacity
     * @param loadFactor
     */
    public ConcurrentCache(int initialCapacity, float loadFactor) {
        this.cache = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 指定初始容量、负载因子和并发级别的构造方法
     * @param initialCapacity
     * @param loadFactor
     * @param concurrencyLevel
     */
    public ConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this.cache = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public V get(K key) {
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
     * 如果键不存在则放入，否则返回已有值
     * @param key
     * @param value
     * @return
     */
    public V putIfAbsent(K key, V value) {
        return cache.putIfAbsent(key, value);
    }

    /**
     * 只有当键存在且等于旧值时，才替换为新值
     * @param key
     * @param oldValue
     * @param newValue
     * @return
     */
    public boolean replace(K key, V oldValue, V newValue) {
        return cache.replace(key, oldValue, newValue);
    }

    /**
     * 如果键不存在，使用函数计算值并放入
     *
     * @param key 键
     * @param mappingFunction 值计算函数
     * @return 计算后的值
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return cache.computeIfAbsent(key, mappingFunction);
    }
}
