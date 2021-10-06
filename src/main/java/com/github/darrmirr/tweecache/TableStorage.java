package com.github.darrmirr.tweecache;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * Table storage contains data storage for particular table at schema
 */
public class TableStorage {
    private Class<?> tableClass;
    private Cache<Object, Object> cache;

    public TableStorage(Class<?> tableClass, Cache<Object, Object> cache) {
        this.tableClass = tableClass;
        this.cache = cache;
    }

    public Cache<Object, Object> getCache() {
        return cache;
    }
}
