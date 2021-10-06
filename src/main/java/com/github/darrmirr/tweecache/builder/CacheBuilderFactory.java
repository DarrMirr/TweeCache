package com.github.darrmirr.tweecache.builder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.CacheBuilder;

import java.util.function.Function;

public class CacheBuilderFactory {

    public static Cache<Object, Object> buildCaffeine(Function<Caffeine<Object, Object>, Cache<Object, Object>> builder) {
        return builder.apply(Caffeine.newBuilder());
    }

    public static com.google.common.cache.Cache<Object, Object> buildGuava(Function<CacheBuilder<Object, Object>, com.google.common.cache.Cache<Object, Object>> builder) {
        return builder.apply(CacheBuilder.newBuilder());
    }
}
