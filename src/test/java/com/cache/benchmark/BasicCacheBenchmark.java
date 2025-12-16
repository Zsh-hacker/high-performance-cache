package com.cache.benchmark;

import com.zsh.cache.Cache;
import com.zsh.cache.SimpleCache;
import com.zsh.cache.SynchronizedCache;
import com.zsh.cache.ConcurrentCache;
import com.zsh.cache.ReenTrantLockCache;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * 基础缓存性能基准测试
 *
 * 测试目标：比较不同线程安全实现的性能差异
 */
@State(Scope.Benchmark)  // 基准测试状态，所有线程共享
@BenchmarkMode(Mode.Throughput)  // 测试吞吐量（单位时间内完成的操作数）
@OutputTimeUnit(TimeUnit.SECONDS)  // 输出时间单位：秒
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)  // 预热：3轮，每轮1秒
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)  // 测量：5轮，每轮2秒
@Fork(2)  // 使用2个独立JVM进程
@Threads(4)  // 使用4个线程并发测试
public class BasicCacheBenchmark {

    // 测试数据
    private String[] testKeys;
    private String[] testValues;

    // 要测试的缓存实例
    private Cache<String, String> simpleCache;
    private Cache<String, String> synchronizedCache;
    private Cache<String, String> reentrantLockCache;
    private Cache<String, String> concurrentCache;

    /**
     * 测试前的准备工作
     * 在每个基准测试方法执行前调用
     */
    @Setup(Level.Trial)  // Trial级别：整个测试过程执行一次
    public void setup() {
        System.out.println("=== 初始化测试环境 ===");

        // 初始化缓存实例
        simpleCache = new SimpleCache<>();
        synchronizedCache = new SynchronizedCache<>();
        reentrantLockCache = new ReenTrantLockCache<>();
        concurrentCache = new ConcurrentCache<>();

        // 初始化测试数据
        int dataSize = 1000;
        testKeys = new String[dataSize];
        testValues = new String[dataSize];

        for (int i = 0; i < dataSize; i++) {
            testKeys[i] = "key-" + i;
            testValues[i] = "value-" + i;
        }

        // 预先填充数据到缓存（模拟真实场景）
        for (int i = 0; i < dataSize; i++) {
            concurrentCache.put(testKeys[i], testValues[i]);
        }

        System.out.println("测试数据初始化完成，大小: " + dataSize);
    }

    /**
     * 测试后的清理工作
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("=== 清理测试环境 ===");
        simpleCache.clear();
        synchronizedCache.clear();
        reentrantLockCache.clear();
        concurrentCache.clear();
    }

    // ========== 基准测试方法 ==========

    /**
     * 测试SimpleCache的get操作（非线程安全）
     */
    @Benchmark
    public void testSimpleCacheGet() {
        int index = getRandomIndex();
        simpleCache.get(testKeys[index]);
    }

    /**
     * 测试SynchronizedCache的get操作
     */
    @Benchmark
    public void testSynchronizedCacheGet() {
        int index = getRandomIndex();
        synchronizedCache.get(testKeys[index]);
    }

    /**
     * 测试ReentrantLockCache的get操作
     */
    @Benchmark
    public void testReentrantLockCacheGet() {
        int index = getRandomIndex();
        reentrantLockCache.get(testKeys[index]);
    }

    /**
     * 测试ConcurrentCache的get操作
     */
    @Benchmark
    public void testConcurrentCacheGet() {
        int index = getRandomIndex();
        concurrentCache.get(testKeys[index]);
    }

    /**
     * 测试SimpleCache的put操作
     */
    @Benchmark
    public void testSimpleCachePut() {
        int index = getRandomIndex();
        simpleCache.put(testKeys[index], testValues[index]);
    }

    /**
     * 测试SynchronizedCache的put操作
     */
    @Benchmark
    public void testSynchronizedCachePut() {
        int index = getRandomIndex();
        synchronizedCache.put(testKeys[index], testValues[index]);
    }

    /**
     * 测试ReentrantLockCache的put操作
     */
    @Benchmark
    public void testReentrantLockCachePut() {
        int index = getRandomIndex();
        reentrantLockCache.put(testKeys[index], testValues[index]);
    }

    /**
     * 测试ConcurrentCache的put操作
     */
    @Benchmark
    public void testConcurrentCachePut() {
        int index = getRandomIndex();
        concurrentCache.put(testKeys[index], testValues[index]);
    }

    /**
     * 测试ConcurrentCache的复合操作（get和put混合）
     */
    @Benchmark
    public void testConcurrentCacheMixed() {
        int index = getRandomIndex();
        if (index % 2 == 0) {
            concurrentCache.get(testKeys[index]);
        } else {
            concurrentCache.put(testKeys[index], testValues[index]);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取随机索引（模拟真实访问模式）
     * 使用ThreadLocalRandom确保线程安全
     */
    private int getRandomIndex() {
        // 使用简单的伪随机，避免Random的同步开销
        // 在实际测试中，可以考虑更复杂的分布（如Zipf分布）
        return Math.abs((int) (Thread.currentThread().getId() + System.nanoTime()) % testKeys.length);
    }

    /**
     * 主方法：运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        System.out.println("=== 开始缓存性能基准测试 ===");
        System.out.println("Current working dir: " + System.getProperty("user.dir"));
        // 配置基准测试选项
        Options options = new OptionsBuilder()
                .include(BasicCacheBenchmark.class.getSimpleName())  // 包含当前类的所有测试
                .exclude(".*SimpleCache.*")  // 排除非线程安全的测试（可选）
                .shouldDoGC(true)  // 在每轮测试间执行GC
                .result("E:\\C桌面\\os\\simple_project\\high-performance-cache\\src\\test\\java\\com\\cache\\benchmark\\benchmark_results\\basic-cache.json")  // 保存结果到文件
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)  // JSON格式
                .build();

        // 运行基准测试
        new Runner(options).run();
    }
}