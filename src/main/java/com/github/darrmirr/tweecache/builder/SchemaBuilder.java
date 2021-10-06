package com.github.darrmirr.tweecache.builder;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.darrmirr.tweecache.TableStorage;
import com.github.darrmirr.tweecache.TweeSchema;
import com.github.darrmirr.tweecache.util.ClassDeclaration;
import com.github.darrmirr.tweecache.util.ClassFactory;
import com.github.darrmirr.tweecache.util.ClassUtils;
import com.github.darrmirr.tweecache.util.Result;
import org.apache.calcite.adapter.java.Array;
import org.apache.calcite.schema.ScalarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Class to build in-memory cache schema
 *
 * Schema name must be unique due to compilation restriction.
 * In other words there is no two classes with the same name at one package.
 */
public class SchemaBuilder {
    private static final Logger log = LoggerFactory.getLogger(SchemaBuilder.class);
    private static final String CLASS_NAME_SUFFIX = "Schema";
    private static final String FIELD_DECLARATION_PATTERN = "@Array(component = %TABLE_CLASS%.class) " +
            "public Collection<%TABLE_CLASS%> %TABLE_NAME%; ";
    private final ClassFactory classFactory = ClassFactory.INSTANCE;
    private final String schemaName;
    private final ClassDeclaration classDeclaration;
    private final Map<String, TableBuilder> tableBuilderMap = new HashMap<>();
    private final List<ScalarFunctionBuilder> scalarFunctionBuilders = new LinkedList<>();

    public SchemaBuilder(String schemaName) {
        this.schemaName = schemaName;
        this.classDeclaration = new ClassDeclaration(TweeSchema.class.getPackage().getName(), schemaName + CLASS_NAME_SUFFIX);
        this.classDeclaration.addImport(Array.class);
        this.classDeclaration.addImport(Collection.class);
    }

    /**
     * Add new table to in-memory cache schema
     *
     * @param tableClass class of row at table
     * @return {@link TableBuilder} instance
     */
    public TableBuilder addTable(Class<?> tableClass) {
        return addTable(tableClass, tableClass.getSimpleName().toLowerCase());
    }

    /**
     * Add new table to in-memory cache schema
     *
     * @param tableClass class of row at table
     * @param tableName table name at schema
     * @return {@link TableBuilder} instance
     */
    public TableBuilder addTable(Class<?> tableClass, String tableName) {
        TableBuilder builder = new TableBuilder(tableClass, tableName, this);
        tableBuilderMap.put(builder.getTableName(), builder);
        String field = FIELD_DECLARATION_PATTERN
                .replaceAll("%TABLE_CLASS%", builder.getTableClass())
                .replace("%TABLE_NAME%", builder.getTableName());
        classDeclaration.addField(field);
        return builder;
    }

    /**
     * Table builder for TweeCache table scheme
     */
    public static class TableBuilder {
        private final Class<?> tableClass;
        private final String tableName;
        private final SchemaBuilder parentBuilder;
        private Cache<Object, Object> cache;

        public TableBuilder(Class<?> tableClass, String tableName, SchemaBuilder parentBuilder) {
            this.tableClass = tableClass;
            this.tableName = tableName;
            this.parentBuilder = parentBuilder;
        }

        /**
         * Build storage for particular table.
         *
         * @param builder storage builder function
         * @return {@link SchemaBuilder} instance
         */
        public SchemaBuilder withStorage(Function<Caffeine<Object, Object>, Cache<Object, Object>> builder) {
            this.cache = CacheBuilderFactory.buildCaffeine(builder);
            return parentBuilder;
        }

        private String getTableName() {
            return tableName;
        }

        private String getTableClass() {
            return tableClass.getName();
        }

        /**
         * Get items collection from underlined cache
         *
         * @return items collection from underlined cache
         */
        private Collection<Object> getCollection() {
            return cache.asMap().values();
        }

        /**
         * Method to create {@link TableStorage} instance from builder
         *
         * @return {@link TableStorage} instance
         */
        private TableStorage toTableCache() {
            return new TableStorage(tableClass, cache);
        }
    }

    /**
     * Create {@link TableStorage} objects from {@link TableBuilder} ones
     *
     * @return map of {@link TableStorage} objects
     */
    private Map<String, TableStorage> createTableStorageMap() {
        Map<String, TableStorage> tableCacheMap = new HashMap<>();
        tableBuilderMap
                .forEach((key, tableBuilder) ->
                        tableCacheMap.put(key, tableBuilder.toTableCache()));
        return tableCacheMap;
    }

    /**
     * Create function that would be treated as stored procedure at SQL-query
     *
     * @param methodClass class that store function's implementation method
     * @param methodName function name that should be used at SQL-query and
     *                   method name that should be invoked if functionName is met at SQL-query
     * @return this {@link SchemaBuilder} instance
     */
    public SchemaBuilder addFunction(Class<?> methodClass, String methodName) {
        return addFunction(methodName, methodClass, methodName);
    }

    /**
     * Create function that would be treated as stored procedure at SQL-query
     *
     * @param functionName function name that should be used at SQL-query as stored procedure
     * @param methodClass class that store function's implementation method
     * @param methodName method name that should be invoked if {@param functionName} is met at SQL-query
     * @return this {@link SchemaBuilder} instance
     */
    public SchemaBuilder addFunction(String functionName, Class<?> methodClass, String methodName) {
        scalarFunctionBuilders.add(new ScalarFunctionBuilder(functionName, methodClass, methodName));
        return this;
    }

    /**
     * {@link SchemaBuilder} termination method to create in-memory cache schema
     *
     * @return {@link TweeSchema} instance
     */
    public Result<TweeSchema> build() {
        return classFactory
                .compile(classDeclaration)
                .flatMap(ClassUtils::newInstance)
                .flatMap(this::linkDataStorages)
                .map(schema -> {
                    Map<String, ScalarFunction> scalarFunctions = scalarFunctionBuilders
                            .stream()
                            .collect(toMap(ScalarFunctionBuilder::getFunctionName, ScalarFunctionBuilder::build));
                    return new TweeSchema(schemaName, schema, scalarFunctions, createTableStorageMap());
                });
    }

    /**
     * Link table store to table at in-memory cache schema
     *
     * @param schema schema with tables
     * @return schema with tables and table storages
     */
    private Result<Object> linkDataStorages(Object schema) {
        for(Field field : schema.getClass().getFields()) {
            String fieldName = field.getName();
            TableBuilder tableBuilder = tableBuilderMap.get(fieldName);
            try {
                log.debug("link data storage to with table '{}'", fieldName);
                field.set(schema, tableBuilder.getCollection());
            } catch (IllegalAccessException e) {
                log.error("error to link data storage with table '{}' due to '{}'.", fieldName, e.getMessage());
                return Result.error(e);
            }
        }
        return Result.ok(schema);
    }
}
