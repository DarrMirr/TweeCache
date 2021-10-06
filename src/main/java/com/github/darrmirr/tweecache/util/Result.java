package com.github.darrmirr.tweecache.util;

import java.lang.module.FindException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result represent monad similar to Either one.
 * Result stores two objects:
 *     - value
 *     - error
 *
 * Value represent valid object is retrieved after function computation
 * Error represent exception that is thrown during computation
 *
 * Expected that only one object will be null.
 *
 * Result should be used as return value at domain specific use cases
 *
 * @param <T> value type
 */
public final class Result<T> {
    private final T value;
    private final Throwable error;
    private Consumer<Throwable> errorConsumer;

    /**
     * Create object with mandatory parameters.
     *
     * Method intentionally has made with private access modifier
     * in order to control its creation process via static methods
     *
     * @param value valid object that is retrieved after function computation
     * @param error exception that is thrown during function computation
     */
    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Create {@link Result} object with valid object
     *
     * Return Error {@link Result} object if T type is subtype of {@link Throwable}
     *
     * @param object valid object that is retrieved after function computation
     * @param <T> value object type
     * @return new {@link Result} object
     */
    public static <T> Result<T> ok(T object) {
        if (object == null) {
            return error(new NullPointerException("value object is null"));
        }
        return object instanceof Throwable ? error((Throwable) object) : new Result<>(object, null);
    }

    /**
     * Create {@link Result} object with error object
     *
     * @param error exception that is thrown during function computation
     * @param <T> error object type
     * @return new {@link Result} object
     */
    public static <T> Result<T> error(Throwable error) {
        return error == null ? new Result<>(null, new NullPointerException("error object is null")) : new Result<>(null, error);
    }

    /**
     * Get value. Method wrap value to {@link Optional} due to this class stores two values
     *
     * @return value
     */
    public Optional<T> get() {
        consumeError();
        return Optional.ofNullable(value);
    }

    /**
     * Get error. Method wrap error to {@link Optional} due to this class stores two values
     *
     * @return error
     */
    public Optional<Throwable> error() {
        consumeError();
        return Optional.ofNullable(error);
    }

    /**
     * Error consumer would be invoked if error is occurred and one of methods to retrieve value or error will be invoked
     *
     * @param errorConsumer consumer function
     * @return this object
     */
    public Result<T> onError(Consumer<Throwable> errorConsumer) {
        this.errorConsumer = errorConsumer;
        return this;
    }

    private boolean isOk() {
        return error == null;
    }

    public <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        Result<U> result;
        try {
            result = isOk() ? Result.ok(mapper.apply(value)) : Result.error(error);
        } catch (Throwable error) {
            result = Result.error(error);
        }
        result.onError(errorConsumer);
        errorConsumer = null;
        return result;
    }

    public <U> Result<U> flatMap(Function<? super T, ? extends Result<U>> mapper) {
        Result<U> result;
        try {
            result = isOk() ? mapper.apply(value) : Result.error(error);
        } catch (Throwable error) {
            result = Result.error(error);
        }
        if (result != null) {
            result.onError(errorConsumer);
            errorConsumer = null;
        }
        return result;
    }

    /**
     * Return value is present or throw runtime exception
     *
     * Method intentionally returns value without {@link Optional} because exception is thrown if value is not present
     *
     * @return value
     */
    public T orElseThrow() {
        if (isOk()) {
            return value;
        }
        consumeError();
        throw new IllegalStateException("Result value is not Ok.", error);
    }

    private void consumeError() {
        if (errorConsumer != null) {
            errorConsumer.accept(error);
        }
    }
}
