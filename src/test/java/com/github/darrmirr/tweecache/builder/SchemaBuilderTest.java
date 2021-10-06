package com.github.darrmirr.tweecache.builder;

import com.github.darrmirr.tweecache.test.model.Department;
import com.github.darrmirr.tweecache.test.model.Employee;
import com.github.darrmirr.tweecache.TweeSchema;
import org.apache.calcite.adapter.java.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaBuilderTest {

    @Test
    void addTable() throws NoSuchFieldException {
        TweeSchema schema = new SchemaBuilder("cacheAddTable")
                .addTable(Employee.class)
                .withStorage(builder ->
                        builder.softValues().build())
                .addTable(Department.class)
                .withStorage(builder ->
                        builder.softValues().build())
                .build()
                .orElseThrow();
        assertEquals(schema.getSchemaName(), "cacheAddTable");
        assertEquals(getArrayComponent(schema, Employee.class.getSimpleName()), Employee.class);
        assertEquals(getArrayComponent(schema, Department.class.getSimpleName()), Department.class);
    }

    private Class<?> getArrayComponent(TweeSchema tweeSchema, String tableName) throws NoSuchFieldException {
        return tweeSchema
                .getSchemaObject()
                .getClass()
                .getField(tableName.toLowerCase())
                .getAnnotation(Array.class)
                .component();
    }
}