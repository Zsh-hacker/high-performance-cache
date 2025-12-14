//package com.zsh.cache;
//
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.atomic.LongAdder;
//
//public class CacheWithHitRate<K, V> implements Cache<K, V>{
//    private final ConcurrentMap<K, V> cache = new ConcurrentHashMap<>();
//    private final LongAdder hitCount = new LongAdder();
//    private final LongAdder missCount = new LongAdder();
//
//    @Override
//    public V get(K key) {
//        V value = cache.get(key);
//        if (value != null) {
//            hitCount.increment();
//        } else {
//            missCount.increment();
//        }
//        return value;
//    }
//
//    public double getHitRate() {
//        long total = hitCount.longValue() + missCount.longValue();
//        return total == 0 ? 0 : (double) hitCount.longValue() / total;
//    }
//
//    public V getOrCreate(K key) {
//        return cache.computeIfAbsent(key, k -> {
//            return expensiveOperation(k);
//        });
//    }
//}
