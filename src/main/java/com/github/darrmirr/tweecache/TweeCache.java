package com.github.darrmirr.tweecache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.darrmirr.tweecache.builder.SchemaBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.calcite.adapter.java.ReflectiveSchema;
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.jdbc.Driver;
import org.apache.calcite.schema.SchemaPlus;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.github.darrmirr.tweecache.util.TweeFunctions.toTableCache;
import static com.github.darrmirr.tweecache.util.TweeFunctions.toTableName;

/**
 * In-memory cache library main class. It supplies set of operation over cache instance.
 */
public class TweeCache {
    private static final Logger log = LoggerFactory.getLogger(TweeCache.class);
    private final TweeSchema tweeSchema;
    private final HikariDataSource dataSource;
    private final Jdbi jdbi;
    private final Function<String, Cache<Object, Object>> toTableCache;
    private final Function<Class<?>, String> toTableName;

    // manually load calcite jdbc Driver if multiply drivers are present in classpath.
    static {
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            log.error("error to register '{}' due to '{}'. See debug log for more details", Driver.class.getName(), e.getMessage());
            log.debug("error to register '" + Driver.class.getName() + "'", e);
        }
    }

    /**
     * Class instance must be created via {@link TweeCacheBuilder}
     *
     * @param tweeSchema cache schema
     * @param dataSource sql data source
     */
    private TweeCache(TweeSchema tweeSchema, HikariDataSource dataSource) {
        this.tweeSchema = tweeSchema;
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource);
        this.toTableCache = toTableCache(tweeSchema::getTableStorage);
        this.toTableName = toTableName();
    }

    /**
     * Attach TweeCache schema to RootSchema at Apache Calcite Connection.
     */
    private void init() {
        try {
            List<Connection> connections = new LinkedList<>();
            ReflectiveSchema schema = new ReflectiveSchema(tweeSchema.getSchemaObject());
            for (int i = 0; i < dataSource.getMaximumPoolSize(); i++) {
                Connection connection = dataSource.getConnection();
                SchemaPlus rootSchema = connection
                        .unwrap(CalciteConnection.class)
                        .getRootSchema();
                rootSchema.add(tweeSchema.getSchemaName().toLowerCase(), schema);
                tweeSchema
                        .getScalarFunctions()
                        .forEach(rootSchema::add);
                connections.add(connection);
            }
            for (Connection connection : connections) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error("error to init cache '{}' due to '{}'. See debug log for more details", tweeSchema.getSchemaName(), e.getMessage());
            log.debug("error to init cache '" + tweeSchema.getSchemaName() + "'", e);
        }
    }

    /**
     * Put value to table cache with specified key.
     * Cache table storage will be found by value class.
     *
     * @param key value's key at cache
     * @param value value to store
     * @param <K> key data type
     * @param <V> value data type
     * @return this {@link TweeCache} instance
     */
    public <K, V> TweeCache put(K key, V value) {
        return put(toTableName, key, value);
    }

    /**
     * Put value to particular cache with specified key.
     *
     * @param tableName table name where value should be stored
     * @param key value's key at cache
     * @param value value to store
     * @param <K> key data type
     * @param <V> value data type
     * @return this {@link TweeCache} instance
     */
    public <K, V> TweeCache put(String tableName, K key, V value) {
        return put(tableClass -> tableName, key, value);
    }

    private  <K, V> TweeCache put(Function<Class<?>, String> toTableName, K key, V value) {
        if (key != null) {
            Optional.ofNullable(value)
                    .map(Object::getClass)
                    .map(toTableName.andThen(toTableCache))
                    .ifPresent(tableCache ->
                            tableCache.put(key, value)
                    );
        }
        return this;
    }

    /**
     * Put all values to table cache.
     * Cache table storage will be found by value class.
     *
     * @param values values to store
     * @param <K> key data type
     * @param <V> value data type
     * @return this {@link TweeCache} instance
     */
    public <K, V> TweeCache putAll(Map<K, V> values) {
        return putAll(toTableName, values);
    }

    /**
     * Put all values to particular table cache.
     *
     * @param tableName table name where values should be stored
     * @param values values to store
     * @param <K> key data type
     * @param <V> value data type
     * @return this {@link TweeCache} instance
     */
    public <K, V> TweeCache putAll(String tableName, Map<K, V> values) {
        return putAll(tableClass -> tableName, values);
    }

    private <K, V> TweeCache putAll(Function<Class<?>, String> toTableName, Map<K, V> values) {
        Optional.ofNullable(values)
                .map(Map::values)
                .flatMap(mapValues ->
                        mapValues.stream().findFirst())
                .map(Object::getClass)
                .map(toTableName.andThen(toTableCache))
                .ifPresent(cache -> cache.putAll(values));
        return this;
    }

    /**
     * Execute SELECT SQL-query against data at in-memory cache without query parameters.
     *
     * @param sqlQuery SQL-query string
     * @param mapper row mapper function
     * @param <T> row item data type
     * @return row item list
     */
    public <T> List<T> query(String sqlQuery, Function<ResultSet, T> mapper) {
        return query(sqlQuery, null, mapper);
    }

    /**
     * Execute SELECT SQL-query against data at in-memory cache with query parameters.
     *
     * @param sqlQuery SQL-query string
     * @param sqlParameters named query parameters
     * @param mapper row mapper function
     * @param <T> row item data type
     * @return row item list
     */
    public <T> List<T> query(String sqlQuery, Map<String, Object> sqlParameters, Function<ResultSet, T> mapper) {
        try(Handle handle = jdbi.open()) {
           return handle
                   .createQuery(sqlQuery)
                   .bindMap(sqlParameters)
                   .map((ResultSet rs, StatementContext ctx) -> mapper.apply(rs))
                   .list();
        }
    }

    /**
     * Get all rows from table cache.
     *
     * @param tableClass table row class
     * @param <T> table row data type
     * @return all row items
     */
    public <T> List<T> getAll(Class<T> tableClass) {
        return (List<T>) Optional
                .ofNullable(tableClass)
                .map(toTableName)
                .flatMap(tweeSchema::getAll)
                .orElseGet(Collections::emptyList);
    }

    /**
     * Get all rows from table cache.
     *
     * @param tableName table name
     * @param <T> table row data type
     * @return all row items
     */
    public <T> List<T> getAll(String tableName) {
        return (List<T>) Optional
                .ofNullable(tableName)
                .flatMap(tweeSchema::getAll)
                .orElseGet(Collections::emptyList);
    }

    /**
     * Get one row from table cache.
     * Value in cache represent row in table.
     * Cache table storage will be found by value class.
     *
     * @param id value's id at cache
     * @param valueClass object's class
     * @param <K> key data type
     * @param <V> value data type
     * @return value
     */
    public <K, V> Optional<V> getById(K id, Class<V> valueClass) {
        return getById(toTableName, id, valueClass);
    }

    /**
     * Get one row from table cache.
     * Value in cache represent row in table.
     *
     * @param tableName table name
     * @param id value's id at cache
     * @param valueClass object's class
     * @param <K> key data type
     * @param <V> value data type
     * @return value
     */
    public <K, V> Optional<V> getById(String tableName, K id, Class<V> valueClass) {
        return getById(tableClass -> tableName, id, valueClass);
    }

    private <K, V> Optional<V> getById(Function<Class<?>, String> toTableName, K id, Class<V> valueClass) {
        if (id == null || valueClass == null) {
            return Optional.empty();
        }
        return Optional
                .of(valueClass)
                .map(toTableName.andThen(toTableCache))
                .map(Cache::asMap)
                .map(cacheMap ->
                        cacheMap.get(id))
                .filter(valueClass::isInstance)
                .map(valueClass::cast);
    }

    /**
     * Get cache statistics from underlined cache implementation.
     *
     * @param tableClass table class
     * @return cache statistics
     */
    public Optional<CacheStats> stats(Class<?> tableClass) {
        return Optional
                .ofNullable(tableClass)
                .map(toTableName)
                .flatMap(tweeSchema::stats);
    }

    /**
     * Get cache statistics from underlined cache implementation
     *
     * @param tableClass table name
     * @return cache statistics
     */
    public Optional<CacheStats> stats(String tableName) {
        return Optional
                .ofNullable(tableName)
                .flatMap(tweeSchema::stats);
    }

    /**
     * Destroy all allocated in-memory cache resources
     *
     * It is recommended to invoke this method before shutdown application
     */
    public void destroy() {
        dataSource.close();
        tweeSchema.invalidateAll();
    }

    public static TweeCacheBuilder builder(String name) {
        return new TweeCacheBuilder(name);
    }

    /**
     * In-memory cache builder
     */
    public static class TweeCacheBuilder {
        private final String schemaName;
        private final SchemaBuilder schemaBuilder;
        private final HikariConfig dataSourceConfig = new HikariConfig();
        private final Properties dataSourceProperties = new Properties();

        public TweeCacheBuilder(String schemaName) {
            this.schemaBuilder = new SchemaBuilder(schemaName);
            this.schemaName = schemaName;
            setUnchangedDataSourceConfig();
        }

        /**
         * Configure in-memory cache schema builder
         *
         * @param builderConsumer schema builder consumer performs actions to configure {@link SchemaBuilder} instance
         * @return this {@link TweeCacheBuilder} instance
         */
        public TweeCacheBuilder withSchema(Consumer<SchemaBuilder> builderConsumer) {
            builderConsumer.accept(schemaBuilder);
            return this;
        }

        /**
         * Configure in-memory cache schema Data Source
         *
         * Internally, Hikari data source pool is used
         *
         * @param dataSourceConfigConsumer data source config consumer performs actions to configure {@link HikariConfig} instance
         * @return this {@link TweeCacheBuilder} instance
         */
        public TweeCacheBuilder withDataSourceConfig(Consumer<HikariConfig> dataSourceConfigConsumer) {
            dataSourceConfigConsumer.accept(dataSourceConfig);
            setUnchangedDataSourceConfig();
            return this;
        }

        /**
         * There are predefined data source configuration that cannot be overwritten.
         * Overwriting this configuration makes Apache Calcite works incorrectly for SQL-query TweeCache capability
         */
        private void setUnchangedDataSourceConfig() {
            dataSourceConfig.setDriverClassName(Driver.class.getName());
            dataSourceConfig.setJdbcUrl(Driver.CONNECT_STRING_PREFIX );
            dataSourceConfig.setPoolName(schemaName + "Pool");
            dataSourceConfig.setPassword(null);
            dataSourceConfig.setUsername(null);
            if (!dataSourceConfig.getDataSourceProperties().contains("lex")) {
                this.dataSourceProperties.put("lex", Lex.MYSQL.name());
                dataSourceConfig.setDataSourceProperties(dataSourceProperties);
            }
        }

        /**
         * Create TweeCache instance according to supplied configuration
         *
         * Method returns Optional due to different kind errors could be occurred during TweeCache creation.
         *
         * @return optional TweeCache instance
         */
        public Optional<TweeCache> build() {
            return schemaBuilder
                    .build()
                    .map(schema -> {
                        TweeCache tweeCache = new TweeCache(schema, new HikariDataSource( dataSourceConfig ));
                        tweeCache.init();
                        return tweeCache;
                    })
                    .onError(throwable -> log.error("error to create " + TweeCache.class.getSimpleName(), throwable))
                    .get();
        }
    }
}
