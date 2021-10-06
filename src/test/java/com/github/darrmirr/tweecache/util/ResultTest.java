package com.github.darrmirr.tweecache.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.*;

class ResultTest {

    @Test
    void leftIdentity() {
        int i = 1;
        Function<Integer, Result<Integer>> square = n -> Result.ok(n * 2);
        Result<Integer> result1 = Result.ok(i).flatMap(square);
        Result<Integer> result2 = square.apply(i);

        assertEquals(result1.orElseThrow(), result2.orElseThrow());
    }

    @Test
    void rightIdentity() {
        Result<Integer> result = Result.ok(1);
        Function<Integer, Result<Integer>> function = Result::ok;
        Result<Integer> newResult = result.flatMap(function);

        assertEquals(result.orElseThrow(), newResult.orElseThrow());
    }

    @Test
    void associativity() {
        Result<Integer> result = Result.ok(1);
        Function<Integer, Result<Integer>> square = n -> Result.ok(n * 2);
        Function<Integer, Result<Integer>> sum = n -> Result.ok(n + 11);

        Result<Integer> result1 = result.flatMap(sum).flatMap(square);
        Result<Integer> result2 = result.flatMap(integer -> sum.apply(integer).flatMap(square));

        assertEquals(result1.orElseThrow(), result2.orElseThrow());
    }

    @Test
    void consumeError() {
        Result<Integer> result = Result.ok(1);
        Function<Integer, Result<Integer>> square = n -> Result.ok(n * 2);

        final AtomicBoolean isInvoke = new AtomicBoolean(false);
        Result<Integer> finalResult = result
                .onError(throwable -> isInvoke.set(true))
                .flatMap(square)
                .map(integer -> Integer.valueOf("test"));

        assertThat(isInvoke.get(), is(false));
        assertThat(finalResult.error().get(), instanceOf(NumberFormatException.class));
        assertThat(isInvoke.get(), is(true));
    }

    @Test
    void okMethodPassException() {
        IllegalStateException exception = new IllegalStateException();
        Result<Object> result = Result.ok(exception);

        assertThat(result.get().isPresent(), is(false));
        assertThat(result.error().get(), is(exception));
    }

    @Test
    void okMethodPassNull() {
        Result<Object> result = Result.ok(null);

        assertThat(result.get().isPresent(), is(false));
        assertThat(result.error().get(), instanceOf(NullPointerException.class));
    }

    @Test
    void errorMethodPassNull() {
        Result<Object> result = Result.error(null);

        assertThat(result.get().isPresent(), is(false));
        assertThat(result.error().get(), instanceOf(NullPointerException.class));
    }

    @Test
    void cleaningErrorConsumer() {
        Result<Integer> result = Result.ok(1);
        Function<Integer, Result<Integer>> square = n -> Result.ok(n * 2);

        final AtomicBoolean isInvoke = new AtomicBoolean(false);
        Result<Integer> resultWithErrorConsumer = result
                .onError(throwable -> isInvoke.set(true));

        Result<Integer> intermediateResult = resultWithErrorConsumer.flatMap(square);

        assertThat(intermediateResult.orElseThrow(), is(2));
        assertThat(resultWithErrorConsumer.error().isPresent(), is(false));
        assertThat(isInvoke.get(), is(false));

        Result<Integer> finalResult = intermediateResult.map(integer -> Integer.valueOf("test"));

        assertThat(intermediateResult.error().isPresent(), is(false));
        assertThat(isInvoke.get(), is(false));

        assertThat(finalResult.error().get(), instanceOf(NumberFormatException.class));
        assertThat(isInvoke.get(), is(true));
    }

}