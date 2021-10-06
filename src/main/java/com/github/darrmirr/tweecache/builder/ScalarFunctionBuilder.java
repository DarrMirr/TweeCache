package com.github.darrmirr.tweecache.builder;

import org.apache.calcite.schema.ScalarFunction;
import org.apache.calcite.schema.impl.ScalarFunctionImpl;

/**
 * Scalar function builder constructs {@link ScalarFunction} instance
 */
public class ScalarFunctionBuilder {
    private final String functionName;
    private final Class<?> methodClass;
    private final String methodName;

    public ScalarFunctionBuilder(Class<?> methodClass, String methodName) {
        this(methodName, methodClass, methodName);
    }

    public ScalarFunctionBuilder(String functionName, Class<?> methodClass, String methodName) {
        this.functionName = functionName;
        this.methodClass = methodClass;
        this.methodName = methodName;
    }

    public ScalarFunction build() {
        return ScalarFunctionImpl.create(methodClass, methodName);
    }

    public String getFunctionName() {
        return functionName;
    }
}
