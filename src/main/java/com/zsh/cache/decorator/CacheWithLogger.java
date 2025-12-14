package com.zsh.cache.decorator;

import com.zsh.cache.Cache;

import java.util.function.BiConsumer;

/**
 * 带日志功能的缓存装饰器
 *
 * 特性：
 * 1. 支持不同日志级别
 * 2. 支持自定义日志处理器
 * 3. 可以动态启用/禁用日志
 * 4. 记录操作耗时
 *
 * @param <K>
 * @param <V>
 */
public class CacheWithLogger<K, V> implements Cache<K, V> {

    /**
     * 日志枚举级别
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    private final Cache<K, V> delegate;
    private final String cacheName;
    private volatile boolean enabled = true;
    private volatile  LogLevel level = LogLevel.INFO;
    private BiConsumer<LogLevel, String> logHandler = this::defaultLogHandler;

    /**
     * 默认构造方法
     * @param delegate
     */
    public CacheWithLogger(Cache<K, V> delegate) {
        this(delegate, "Cache");
    }

    public CacheWithLogger(Cache<K, V> delegate, String cacheName) {
        if (delegate == null) {
            throw new IllegalArgumentException("被装饰的缓存不能为null");
        }
        this.delegate = delegate;
        this.cacheName = cacheName != null ? cacheName : "Cache";
    }

    @Override
    public V get(K key) {
        if (!enabled || level.ordinal() > LogLevel.INFO.ordinal()) {
            return delegate.get(key);
        }

        long startTime = System.nanoTime();
        try {
            V value = delegate.get(key);
            long duration = System.nanoTime() - startTime;

            if (value != null) {
                log(LogLevel.INFO, String.format("GET hit: key=%s, value=%s, time=%dns",
                        key, abbreviate(value), duration));
            } else {
                log(LogLevel.INFO, String.format("GET miss: key=%s, time=%dns",
                        key, duration));
            }
            return value;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log(LogLevel.ERROR, String.format("GET error: key=%s, error=%s, time=%dns",
                    key, e.getMessage(), duration));
            throw e;
        }
    }

    @Override
    public void put(K key, V value) {
        if (!enabled || level.ordinal() > LogLevel.INFO.ordinal()) {
            delegate.put(key, value);
            return;
        }

        long startTime = System.nanoTime();
        try {
            delegate.put(key, value);
            long duration = System.nanoTime() - startTime;

            log(LogLevel.INFO, String.format("PUT: key=%s, value=%s, time=%dns",
                    key, abbreviate(value), duration));
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log(LogLevel.ERROR, String.format("PUT error: key=%s, value=%s, error=%s, time=%dns",
                    key, abbreviate(value), e.getMessage(), duration));
            throw e;
        }
    }

    @Override
    public void remove(K key) {
        if (!enabled || level.ordinal() > LogLevel.INFO.ordinal()) {
            delegate.remove(key);
            return;
        }

        long startTime = System.nanoTime();
        try {
            delegate.remove(key);
            long duration = System.nanoTime() - startTime;

            log(LogLevel.INFO, String.format("REMOVE: key=%s, time=%dns", key, duration));
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log(LogLevel.ERROR, String.format("REMOVE error: key=%s, error=%s, time=%dns",
                    key, e.getMessage(), duration));
            throw e;
        }
    }

    @Override
    public void clear() {
        if (!enabled || level.ordinal() > LogLevel.WARN.ordinal()) {
            delegate.clear();
            return;
        }

        long startTime = System.nanoTime();
        try {
            int sizeBefore = delegate.size();
            delegate.clear();
            long duration = System.nanoTime() - startTime;

            log(LogLevel.WARN, String.format("CLEAR: cleared %d entries, time=%dns",
                    sizeBefore, duration));
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log(LogLevel.ERROR, String.format("CLEAR error: error=%s, time=%dns",
                    e.getMessage(), duration));
            throw e;
        }
    }

    @Override
    public int size() {
        if (!enabled || level.ordinal() > LogLevel.DEBUG.ordinal()) {
            return delegate.size();
        }

        long startTime = System.nanoTime();
        try {
            int size = delegate.size();
            long duration = System.nanoTime() - startTime;

            log(LogLevel.DEBUG, String.format("SIZE: %d, time=%dns", size, duration));
            return size;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            log(LogLevel.ERROR, String.format("SIZE error: error=%s, time=%dns",
                    e.getMessage(), duration));
            throw e;
        }
    }

    // ========== 日志相关方法 ==========
    /**
     * 记录日志
     */
    private void log(LogLevel level, String message) {
        if (enabled && this.level.ordinal() <= level.ordinal()) {
            String formatted = String.format("[%s] %s: %s",
                    cacheName, level, message);
            logHandler.accept(level, formatted);
        }
    }

    /**
     * 默认日志处理器（输出到控制台）
     */
    private void defaultLogHandler(LogLevel level, String message) {
        System.out.println(message);
    }

    /**
     * 截断过长的值（防止日志过大）
     */
    private String abbreviate(Object value) {
        if (value == null) return "null";

        String str = value.toString();
        if (str.length() <= 50) return str;

        return str.substring(0, 47) + "...";
    }

    // ========== 配置方法 ==========

    /**
     * 启用/禁用日志
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 设置日志级别
     */
    public void setLogLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.INFO;
    }

    /**
     * 设置自定义日志处理器
     */
    public void setLogHandler(BiConsumer<LogLevel, String> logHandler) {
        this.logHandler = logHandler != null ? logHandler : this::defaultLogHandler;
    }

    /**
     * 获取缓存名称
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * 是否启用日志
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取当前日志级别
     */
    public LogLevel getLogLevel() {
        return level;
    }
}
