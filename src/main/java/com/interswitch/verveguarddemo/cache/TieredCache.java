package com.interswitch.verveguarddemo.cache;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class TieredCache implements org.springframework.cache.Cache {

    @Getter
    private final org.springframework.cache.Cache l1;
    private final org.springframework.cache.Cache l2;

    private final ConcurrentHashMap<Object, Object> keyLocks = new ConcurrentHashMap<>();

    public TieredCache(org.springframework.cache.Cache l1, org.springframework.cache.Cache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    @NonNull
    public String getName() {
        return l2.getName();
    }

    @Override
    @NonNull
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper v = l1.get(key);
        if (v != null) return v;

        v = l2.get(key);
        if (v != null) {
            l1.put(key, v.get()); // backfill L1
        }
        return v;
    }

    @Override
    public <T> T get(@NonNull Object key, Class<T> type) {
        T v = l1.get(key, type);
        if (v != null) return v;

        v = l2.get(key, type);
        if (v != null) {
            l1.put(key, v);
        }
        return v;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        // L1 hit
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) return (T) wrapper.get();

        // L2 hit — backfill L1
        wrapper = l2.get(key);
        if (wrapper != null) {
            T v = (T) wrapper.get();
            l1.put(key, v);
            return v;
        }

        // True miss — only one thread loads per key, others wait
        Object lock = keyLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            try {
                // Double-check after acquiring lock — another thread may have loaded it
                wrapper = l1.get(key);
                if (wrapper != null) return (T) wrapper.get();

                wrapper = l2.get(key);
                if (wrapper != null) {
                    T v = (T) wrapper.get();
                    l1.put(key, v);
                    return v;
                }

                // Still a miss — we're the loader
                T v = valueLoader.call();
                l1.put(key, v);
                l2.put(key, v);
                return v;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            } finally {
                keyLocks.remove(key);
            }
        }
    }


    @Override
    public void put(@NonNull Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) return wrapper;
        put(key, value);
        return null;
    }

    @Override
    public void evict(@NonNull Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        boolean evictedFromL1 = l1.evictIfPresent(key);
        boolean evictedFromL2 = l2.evictIfPresent(key);
        return evictedFromL1 || evictedFromL2;
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }

    @Override
    public boolean invalidate() {
        l1.invalidate();
        l2.invalidate();
        return true;
    }
}