package com.cache.benchmark;

import com.zsh.cache.Cache;
import com.zsh.cache.ConcurrentCache;
import com.zsh.cache.OptimizedConcurrentCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 优化效果对比基准测试
 * 对比原始实现和优化实现的性能差异
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class OptimizationBenchmark {

    // 测试不同的缓存实现
    private Cache<String, String> originalCache;
    private Cache<String, String> optimizedCache;
    private ConcurrentHashMap<String, String> concurrentHashMap;

    // 测试数据
    private List<String> keys;
    private static final int DATA_SIZE = 10000;
    private static final int HOT_SPOT_SIZE = 100;  // 热点数据数量

    @Setup(Level.Trial)
    public void setup() {
        System.out.println("初始化优化对比测试...");

        // 初始化缓存实例
        originalCache = new ConcurrentCache<>();
        optimizedCache = new OptimizedConcurrentCache<>();
        concurrentHashMap = new ConcurrentHashMap<>();

        // 生成测试数据
        keys = new ArrayList<>(DATA_SIZE + HOT_SPOT_SIZE);

        // 普通数据
        for (int i = 0; i < DATA_SIZE; i++) {
            String key = "key-" + i;
            keys.add(key);
        }

        // 热点数据
        for (int i = 0; i < HOT_SPOT_SIZE; i++) {
            String key = "hot-key-" + i;
            keys.add(key);
        }

        // 预填充数据（80%的填充率）
        int fillCount = (int) (keys.size() * 0.8);
        for (int i = 0; i < fillCount; i++) {
            String key = keys.get(i);
            String value = "value-" + i;

            originalCache.put(key, value);
            optimizedCache.put(key, value);
            concurrentHashMap.put(key, value);
        }

        System.out.printf("测试数据初始化完成: 总数据量=%d, 填充量=%d%n",
                keys.size(), fillCount);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        originalCache.clear();
        optimizedCache.clear();
        concurrentHashMap.clear();
    }

    // ========== 基础操作性能对比 ==========

    @Benchmark
    @Threads(1)
    public void testOriginalCacheGetSingleThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = originalCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(1)
    public void testOptimizedCacheGetSingleThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = optimizedCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(1)
    public void testConcurrentHashMapGetSingleThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = concurrentHashMap.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(4)
    public void testOriginalCacheGetMultiThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = originalCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(4)
    public void testOptimizedCacheGetMultiThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = optimizedCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(4)
    public void testConcurrentHashMapGetMultiThread(Blackhole blackhole) {
        String key = getRandomKey();
        String value = concurrentHashMap.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(8)
    public void testOriginalCacheGetHighConcurrency(Blackhole blackhole) {
        String key = getRandomKey();
        String value = originalCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(8)
    public void testOptimizedCacheGetHighConcurrency(Blackhole blackhole) {
        String key = getRandomKey();
        String value = optimizedCache.get(key);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(8)
    public void testConcurrentHashMapGetHighConcurrency(Blackhole blackhole) {
        String key = getRandomKey();
        String value = concurrentHashMap.get(key);
        blackhole.consume(value);
    }

    // ========== 写操作性能对比 ==========

    @Benchmark
    @Threads(4)
    public void testOriginalCachePut(Blackhole blackhole) {
        String key = getRandomKey();
        String value = "new-value-" + System.nanoTime();
        originalCache.put(key, value);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(4)
    public void testOptimizedCachePut(Blackhole blackhole) {
        String key = getRandomKey();
        String value = "new-value-" + System.nanoTime();
        optimizedCache.put(key, value);
        blackhole.consume(value);
    }

    @Benchmark
    @Threads(4)
    public void testConcurrentHashMapPut(Blackhole blackhole) {
        String key = getRandomKey();
        String value = "new-value-" + System.nanoTime();
        concurrentHashMap.put(key, value);
        blackhole.consume(value);
    }

    // ========== 复合操作性能对比 ==========

    @Benchmark
    @Threads(4)
    public void testOriginalCacheMixed(Blackhole blackhole) {
        String key = getRandomKey();
        if (ThreadLocalRandom.current().nextBoolean()) {
            // 读操作
            String value = originalCache.get(key);
            blackhole.consume(value);
        } else {
            // 写操作
            String value = "update-" + System.nanoTime();
            originalCache.put(key, value);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @Threads(4)
    public void testOptimizedCacheMixed(Blackhole blackhole) {
        String key = getRandomKey();
        if (ThreadLocalRandom.current().nextBoolean()) {
            String value = optimizedCache.get(key);
            blackhole.consume(value);
        } else {
            String value = "update-" + System.nanoTime();
            optimizedCache.put(key, value);
            blackhole.consume(value);
        }
    }

    @Benchmark
    @Threads(4)
    public void testConcurrentHashMapMixed(Blackhole blackhole) {
        String key = getRandomKey();
        if (ThreadLocalRandom.current().nextBoolean()) {
            String value = concurrentHashMap.get(key);
            blackhole.consume(value);
        } else {
            String value = "update-" + System.nanoTime();
            concurrentHashMap.put(key, value);
            blackhole.consume(value);
        }
    }

    // ========== 热点数据访问测试 ==========

    @Benchmark
    @Threads(4)
    public void testHotSpotAccess(Blackhole blackhole) {
        // 80%的访问集中在热点数据
        String key;
        if (ThreadLocalRandom.current().nextDouble() < 0.8) {
            // 访问热点数据
            int index = DATA_SIZE + ThreadLocalRandom.current().nextInt(HOT_SPOT_SIZE);
            key = keys.get(index);
        } else {
            // 访问普通数据
            key = getRandomKey();
        }

        String value = optimizedCache.get(key);
        blackhole.consume(value);
    }

    // ========== 统计开销测试 ==========

    @Benchmark
    @Threads(4)
    public void testWithStatsOverhead(Blackhole blackhole) {
        // 测试统计功能对性能的影响
        String key = getRandomKey();

        // 记录开始时间
        long startTime = System.nanoTime();

        // 执行操作
        String value = optimizedCache.get(key);

        // 记录结束时间
        long endTime = System.nanoTime();

        blackhole.consume(value);
        blackhole.consume(endTime - startTime);
    }

    // ========== 辅助方法 ==========

    /**
     * 获取随机key（模拟真实访问模式）
     */
    private String getRandomKey() {
        int index = ThreadLocalRandom.current().nextInt(keys.size());
        return keys.get(index);
    }

    /**
     * 运行特定测试场景
     */
    public static void runSpecificTest(String testName) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(OptimizationBenchmark.class.getSimpleName() + "." + testName)
                .shouldDoGC(true)
                .result("benchmark-results/optimization-" + testName + ".json")
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .build();

        new Runner(options).run();
    }

    public static void main(String[] args) throws RunnerException {
        System.out.println("=== 开始优化效果对比基准测试 ===");

        // 可以运行所有测试或特定测试
        if (args.length > 0 && "single".equals(args[0])) {
            // 只运行单线程测试
            runSpecificTest("test.*SingleThread");
        } else if (args.length > 0 && "multi".equals(args[0])) {
            // 只运行多线程测试
            runSpecificTest("test.*MultiThread");
        } else if (args.length > 0 && "high".equals(args[0])) {
            // 只运行高并发测试
            runSpecificTest("test.*HighConcurrency");
        } else {
            // 运行所有测试
            Options options = new OptionsBuilder()
                    .include(OptimizationBenchmark.class.getSimpleName())
                    .shouldDoGC(true)
                    .result("E:\\C桌面\\os\\simple_project\\high-performance-cache\\src\\test\\java\\com\\cache\\benchmark\\benchmark_results\\optimization-all.json")
                    .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                    .build();

            new Runner(options).run();
        }
    }
}