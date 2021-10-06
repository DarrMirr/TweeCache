package com.github.darrmirr.tweecache.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.darrmirr.tweecache.TableStorage;

import java.util.Optional;
import java.util.function.Function;

/**
 * Storage for library functions
 */
public final class TweeFunctions {

    private TweeFunctions() { }

    public static Function<Class<?>, String> toTableName() {
        return valueClass -> Optional
                .ofNullable(valueClass)
                .map(Class::getSimpleName)
                .map(String::toLowerCase)
                .orElse(null);
    }

    public static Function<String, Cache<Object, Object>> toTableCache(Function<String, Optional<TableStorage>> tableStorageSupplier) {
        return  tableName -> Optional
                .ofNullable(tableName)
                .flatMap(tableStorageSupplier)
                .map(TableStorage::getCache)
                .orElse(null);
    }
}
