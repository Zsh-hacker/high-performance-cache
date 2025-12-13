package com.zsh.cache;

/**
 * 缓存接口 - 定义缓存的基本操作
 * @param <K>
 * @param <V>
 */
public interface Cache<K, V> {
    /**
     * 将键值对放入缓存
     * @param key
     * @param value
     */
    void put(K key, V value);

    /**
     * 根据键获取值
     * @param key
     * @return 对应的值，如果不存在返回null
     */
    V get(K key);

    /**
     * 根据键移除缓存项
     * @param key
     */
    void remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存中键值对的数量
     * @return  缓存大小
     */
    int size();

}
