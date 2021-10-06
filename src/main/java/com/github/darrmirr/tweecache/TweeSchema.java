package com.github.darrmirr.tweecache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.calcite.schema.ScalarFunction;

import java.util.*;

/**
 * Schema describes data structure, function to operate over data and contains data storage.
 */
public class TweeSchema {
    private final String schemaName;
    private final Object schemaObject;
    private final Map<String, ScalarFunction> scalarFunctions;
    private final Map<String, TableStorage> tableStorageMap;

    public TweeSchema(String schemaName, Object schemaObject, Map<String, ScalarFunction> scalarFunctions, Map<String, TableStorage> tableStorageMap) {
        this.schemaName = schemaName;
        this.schemaObject = schemaObject;
        this.scalarFunctions = scalarFunctions;
        this.tableStorageMap = tableStorageMap;
    }

    /**
     * Get schema name
     *
     * @return schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Get schema object
     *
     * @return shema object
     */
    public Object getSchemaObject() {
        return schemaObject;
    }

    /**
     * Get user defined scalar functions map
     *
     * @return user defined scalar functions map
     */
    protected Map<String, ScalarFunction> getScalarFunctions() {
        return scalarFunctions;
    }

    /**
     * Get table storage object
     *
     * @param name table name
     * @return table storage instance
     */
    public Optional<TableStorage> getTableStorage(String name) {
        return Optional
                .ofNullable(name)
                .map(tableStorageMap::get);
    }

    /**
     * Get table storage statistics.
     * Statistics depend on table storage implementation.
     *
     * @param name table name
     * @return table storage statistics
     */
    public Optional<CacheStats> stats(String name) {
        return Optional
                .ofNullable(name)
                .flatMap(this::getTableStorage)
                .map(TableStorage::getCache)
                .map(Cache::stats);
    }

    /**
     * Invalidate all table storages
     */
    public void invalidateAll() {
        tableStorageMap
                .values()
                .stream()
                .map(TableStorage::getCache)
                .forEach(Cache::invalidateAll);
    }

    /**
     * Get all objects in table
     *
     * @param name table name
     * @return list objects stored in table
     */
    public Optional<List<Object>> getAll(String name) {
        return Optional
                .ofNullable(name)
                .flatMap(this::getTableStorage)
                .map(TableStorage::getCache)
                .map(Cache::asMap)
                .map(Map::values)
                .map(LinkedList::new);
    }
}
