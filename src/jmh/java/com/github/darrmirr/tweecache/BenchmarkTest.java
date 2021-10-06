package com.github.darrmirr.tweecache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.darrmirr.tweecache.test.model.Department;
import com.github.darrmirr.tweecache.test.model.Employee;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.darrmirr.tweecache.test.factory.EmployeeFactory.createEmployee1;
import static com.github.darrmirr.tweecache.test.factory.EmployeeFactory.employeeRowMapper;

public class BenchmarkTest {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(BenchmarkTest.class.getSimpleName() + ".*")
                .build();
        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkCache {
        public TweeCache tweeCache;

        @Setup
        public void setUp() {
            tweeCache = TweeCache
                    .builder("cacheBenchmark")
                    .withSchema(schemaBuilder ->
                            schemaBuilder
                                    .addTable(Employee.class)
                                    .withStorage(Caffeine::build)
                                    .addTable(Department.class)
                                    .withStorage(Caffeine::build)
                    )
                    .build()
                    .orElseThrow(IllegalStateException::new);
            Employee employee = createEmployee1();
            tweeCache.put(employee.id, employee);
        }

        @TearDown
        public void tearDown() {
            Optional.ofNullable(tweeCache)
                    .ifPresent(TweeCache::destroy);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public List<Employee> selectQuery(BenchmarkCache cache) throws InterruptedException {
        return cache.tweeCache.query("select emp.* from cacheBenchmark.employee as emp", employeeRowMapper());
    }
}
