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
            l1.put(key, v); // backfill L1
        }
        return v;
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        T v = l1.get(key, (Class<T>) Object.class);
        if (v != null) return v;

        v = l2.get(key, valueLoader);
        if (v != null) {
            l1.put(key, v); // backfill L1
        }
        return v;
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public void evict(@NonNull Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}