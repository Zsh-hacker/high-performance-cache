package com.zsh.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用ReentrantLock实现线程安全的缓存
 * 优点：
 * 1. 可中断的锁获取
 * 2. 尝试锁（带超时时间）
 * 3. 公平锁选项
 * 4. 多个条件队列
 *
 * 缺点：
 * 1. 需要手动释放锁（容易忘记）
 * 2. 代码更复杂
 * @param <K>
 * @param <V>
 */
public class ReenTrantLockCache<K, V> implements Cache<K, V>{
    private final Map<K, V> cache;
    private final Lock lock;

    /**
     * 默认构造方法 - 使用非公平锁
     */
    public ReenTrantLockCache() {
        this(false);
    }

    /**
     * 指定公平性的构造方法
     * @param fair
     */
    public ReenTrantLockCache(boolean fair) {
        this.cache = new HashMap<>();
        this.lock = new ReentrantLock(fair);
    }

    /**
     * 指定公平性和初始容量的构造方法
     * @param initialCapacity
     * @param fair
     */
    public ReenTrantLockCache(int initialCapacity, boolean fair) {
        this.cache = new HashMap<>(initialCapacity);
        this.lock = new ReentrantLock(fair);
    }

    @Override
    public void put(K key, V value) {
        lock.lock();
        try {
            cache.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V get(K key) {
        lock.lock();
        try {
            return cache.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(K key) {
        lock.lock();
        try {
            cache.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();;
        try {
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return cache.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试获取锁，带超时时间
     * @param key
     * @param value
     * @param timeout   超时时间（毫秒）
     * @return  是否成功放入
     * @throws InterruptedException 如果被中断
     */

    public boolean tryPut(K key, V value, long timeout) throws InterruptedException {
        if(lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            try {
                cache.put(key, value);
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;   // 获取锁失败
    }

    /**
     * 可终端的获取值
     * @param key
     * @return  值
     * @throws InterruptedException 如果被中断
     */
    public V interruptibleGet(K key) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            return cache.get(key);
        } finally {
            lock.unlock();
        }
    }
}
