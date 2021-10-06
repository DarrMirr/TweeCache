package com.github.darrmirr.tweecache.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public final class ClassUtils {

    private ClassUtils() { }

    /**
     * Create new instance of provided class
     *
     * @return new object
     */
    public static <T> Result<T> newInstance(Class<? extends T> tClass) {
        return Optional
                .ofNullable(tClass)
                .map(clazz -> {
                    try {
                        return Result.<T>ok(clazz.getDeclaredConstructor().newInstance());
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        return Result.<T>error(e);
                    }
                })
                .orElseGet(() -> Result.error(new NullPointerException("error to create class instance due to class object is null")));
    }
}
