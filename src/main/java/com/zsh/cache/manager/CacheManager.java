package com.zsh.cache.manager;

import com.zsh.cache.Cache;
import com.zsh.cache.decorator.CacheWithStats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存管理器
 * 统一管理所有缓存实例，提供监控、统计、生命周期管理
 */
public class CacheManager {
    private static CacheManager instance;
    private final Map<String, Cache<?, ?>> caches;
    private final ScheduledExecutorService monitorExecutor;
    private volatile boolean monitoringEnabled = false;
    private final AtomicLong totalHits = new AtomicLong();
    private final AtomicLong totalMisses = new AtomicLong();

    /**
     * 私有构造方法（单例模式）
     */
    private CacheManager() {
        this.caches = new ConcurrentHashMap<>();
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "CacheManager-Monitor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 获取单例实例
     */
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    // ========== 缓存管理方法 ==========
    /**
     * 注册缓存
     */
    public <K, V> void registerCache(String name, Cache<K, V> cache) {
        if (name == null || name.trim().isEmpty()) {
            throw  new IllegalArgumentException("缓存名称不能为空");
        }
        if (cache == null) {
            throw new IllegalArgumentException("缓存实例不能为null");
        }
        caches.put(name, cache);
        System.out.println("缓存已注册：" + name);
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.get(name);
    }

    /**
     * 移除缓存
     */
    public void removeCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            System.out.println("缓存已移除：" + name);
        }
    }

    /**
     * 获取所有缓存名称
     */
    public Set<String> getAllCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        for (Cache<?, ?> cache : caches.values()) {
            cache.clear();
        }
        System.out.println("所有缓存已清空");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, CacheStats> getCacheStats() {
        Map<String, CacheStats> stats = new HashMap<>();

        for (Map.Entry<String, Cache<?, ?>> entry : caches.entrySet()) {
            String name = entry.getKey();
            Cache<?, ?> cache = entry.getValue();

            if (cache instanceof CacheWithStats) {
                CacheWithStats<?, ?> cacheWithStats = (CacheWithStats<?, ?>) cache;
                stats.put(name, new CacheStats(
                        cacheWithStats.getHitCount(),
                        cacheWithStats.getMissCount(),
                        cacheWithStats.getHitRate(),
                        cache.size()
                ));
            } else {
                // 对于没有统计的缓存，返回基本信息
                stats.put(name, new CacheStats(0, 0, 0.0, cache.size()));
            }
        }

        return stats;
    }

    /**
     * 打印所有缓存统计信息
     */
    public void printAllStats() {
        Map<String, CacheStats> stats = getCacheStats();

        System.out.println("=== 缓存管理器统计 ===");
        System.out.println("缓存总数: " + caches.size());

        long totalSize = 0;
        long totalHits = 0;
        long totalMisses = 0;

        for (Map.Entry<String, CacheStats> entry : stats.entrySet()) {
            String name = entry.getKey();
            CacheStats cacheStats = entry.getValue();

            totalSize += cacheStats.size;
            totalHits += cacheStats.hits;
            totalMisses += cacheStats.misses;

            System.out.printf("缓存 [%s]: 大小=%d, 命中=%d, 未命中=%d, 命中率=%.1f%%%n",
                    name, cacheStats.size, cacheStats.hits, cacheStats.misses,
                    cacheStats.hitRate * 100);
        }

        long totalAccess = totalHits + totalMisses;
        double overallHitRate = totalAccess == 0 ? 0.0 : (double) totalHits / totalAccess;

        System.out.println("=== 汇总统计 ===");
        System.out.printf("总大小: %d, 总访问: %d, 总命中率: %.1f%%%n",
                totalSize, totalAccess, overallHitRate * 100);
    }

    // ========== 监控功能 ==========

    /**
     * 开始监控
     *
     * @param interval 监控间隔（秒）
     */
    public void startMonitoring(int interval) {
        if (monitoringEnabled) {
            System.out.println("监控已经在运行");
            return;
        }

        monitoringEnabled = true;
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                System.out.println("=== 缓存监控报告 ===");
                System.out.println("时间: " + new Date());
                printAllStats();
                System.out.println();
            } catch (Exception e) {
                System.err.println("监控任务异常: " + e.getMessage());
            }
        }, 0, interval, TimeUnit.SECONDS);

        System.out.println("缓存监控已启动，间隔: " + interval + "秒");
    }

    /**
     * 停止监控
     */
    public void stopMonitoring() {
        monitoringEnabled = false;
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("缓存监控已停止");
    }

    /**
     * 关闭缓存管理器（释放资源）
     */
    public void shutdown() {
        stopMonitoring();
        clearAllCaches();
        caches.clear();
        System.out.println("缓存管理器已关闭");
    }

    // ========== 内部类 ==========

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final long hits;
        public final long misses;
        public final double hitRate;
        public final int size;

        public CacheStats(long hits, long misses, double hitRate, int size) {
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.size = size;
        }
    }
}
