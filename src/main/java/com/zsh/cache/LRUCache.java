package com.zsh.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于LinkedHashMap实现的LRU缓存
 *
 * 原理：
 * 1. LinkedHashMap维护了插入顺序或访问顺序
 * 2. accessOrder=true时按访问顺序排序（最近访问的放在最后）
 * 3. 重写removeEldestEntry方法控制是否移除最老元素
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> implements Cache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, V> map;

    /**
     * 构造方法
     * @param capacity  缓存容量，超过此容量将触发淘汰
     */
    public LRUCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("容量必须大于0");
        }

        this.capacity = capacity;
        this.map = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public void remove(K key) {
        map.remove( key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 获取缓存容量
     * @return
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 获取缓存中最近最少使用的键
     * @return
     */
    public K getEldestKey() {
        if (map.isEmpty()) {
            return null;
        }

        return map.entrySet().iterator().next().getKey();
    }

    /**
     * 打印缓存内容（用于调试）
     */
    public void printCache() {
        System.out.print("LRU缓存 (最近访问 → 最久未访问): ");
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.print("[" + entry.getKey() + "=" + entry.getValue() + "] ");
        }
        System.out.println();
    }
}
