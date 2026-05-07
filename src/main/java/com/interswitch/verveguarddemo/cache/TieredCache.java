package com.interswitch.verveguarddemo.cache;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.Callable;

public class TieredCache implements org.springframework.cache.Cache {

    @Getter
    private final org.springframework.cache.Cache l1;
    private final org.springframework.cache.Cache l2;

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
        // Check L1 via ValueWrapper to preserve type info
        ValueWrapper wrapper = l1.get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        // Check L2 via ValueWrapper before invoking loader
        wrapper = l2.get(key);
        if (wrapper != null) {
            T v = (T) wrapper.get();
            l1.put(key, v); // backfill L1
            return v;
        }

        // True cache miss — invoke the loader (hits DB)
        try {
            T v = valueLoader.call();
            l1.put(key, v);
            l2.put(key, v);
            return v;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
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