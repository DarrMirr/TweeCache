## TweeCache
In-memory cache with SQL-query capability
***

### Installation

**Gradle**

1. Download jar file with dependencies (-all suffix at file name) and put it to some directory, for example, at `${project.root.dir}/libs`
2. Add dependency to your project: 
```kotlin
 repositories {
     flatDir {
         dirs('libs')
     }
 }

 dependencies {
     implementation 'com.github.darrmirr:tweecache:1.0.0:all'
 }   
```

### Usage

There are 3 simple steps to start using TweeCache:

1. Create TweeCache instance
```java
TweeCache tweeCache = TweeCache
        .builder("cache")                               // set unique schema name
        .withSchema(builder ->                          // create cache schema
                    builder
                        .addTable(Employee.class)       // add table to schema
                        .withStorage(Caffeine::build)   // link data storage to table
                        .addTable(Department.class)
                        .withStorage(Caffeine::build))
        .build()                                        // build TweeCache
        .orElseThrow(IllegalStateException::new);       // throw exception if something goes wrong
```
2. Put your data to TweeCache
```java
tweeCache
    .put(employee1.id, employee1)
    .put(employee2.id, employee2)
    .put(employee3.id, employee3)
    .put(department1.id, department1)
    .put(department2.id, department2);
```
3. Execute SQL-query
```java
String sqlQuery = "select emp.* from cache.employee emp " +
                  "left join cache.department as dep on emp.departmentId = dep.id " +
                  "where dep.name = :dep_name";
List<Employee> employeeList = tweeCache.query(sqlQuery, singletonMap("dep_name", "IT department"), employeeRowMapper());
```

See more usage examples at [com.github.darrmirr.tweecache.TweeCacheTest](https://github.com/DarrMirr/TweeCache/blob/main/src/test/java/com/github/darrmirr/tweecache/TweeCacheTest.java) class

### Minimum requirements:

*Runtime:*
- JRE 8

*Development:*
- JDK 8
- Gradle 7

### TweeCache under hood

Internally, TweeCache use [Caffeine](https://github.com/ben-manes/caffeine) Java library to store data and [Apache Calcite](https://calcite.apache.org) Java library to execute SQL-queries.

Some useful links:
- [Apache Calcite SQL dialect](https://calcite.apache.org/docs/reference.html)

#### Performance recommendation

Parsing SQL-query, performing validation and building query plan requires time to execute. Therefore, all executed SQL-queries are stored at internal cache. Cache item's lifetime is equal to 15 minutes after last access. Therefore, first SQL-query execution requires more time than next one. 

**How to hit to SQL-query cache?**

Let's assume there are two SQL queries:

First SQL-query:
```sqlite-psql
select emp.* from cache.employee as emp where emp.lastName = :lastName;
```
Second SQL-query:
```sqlite-psql
select emp.* from cache.employee as emp where emp.age = :age;
```
SQL-queries cache key evaluates as hash from SQL-query string. Therefore, each SQL-query would be treated as separate queries. Another word, there is separate item at SQL-quires cache for each SQL-query.

Let's combine two queries to one:
```sqlite-psql
select emp.* 
from cache.employee as emp 
where 
(:filterByLastName = false or emp.lastName = :lastName) and 
(:filterByAge = false or emp.age = :age);
```
Query became more complicated than previous one. But it has capability to perform filtering by several parameters. And SQL-query cache will store only one item for such sql query.

**Why is SQL-queries caching needed?**

Here [Java microbenchmark harness](https://openjdk.java.net/projects/code-tools/jmh/) log output:

```
# Warmup Iteration   1: 2,712 ms/op
# Warmup Iteration   2: 0,234 ms/op
# Warmup Iteration   3: 0,140 ms/op
# Warmup Iteration   4: 0,137 ms/op
# Warmup Iteration   5: 0,136 ms/op
Iteration   1: 0,137 ms/op
Iteration   2: 0,137 ms/op
Iteration   3: 0,138 ms/op
Iteration   4: 0,136 ms/op
Iteration   5: 0,136 ms/op
```

And simple benchmark results:
```
Benchmark                  Mode  Cnt  Score   Error  Units
BenchmarkTest.selectQuery  avgt   25  0,144 ± 0,007  ms/op
```

Non-cached query is executed for 2 712 ms. But all cached queries executed average for 0,144 ms. Execution time depends on a lot of options but non-cached query is slower than cached one.

### FAQ

I collect some questions about TweeCache. 

1. Is TweeCache in-memory database?

Answer: It does not.
2. Does TweeCache support indexes?

Answer: 
As I know, Apache Calcite does not have indexes implementation. Perhaps, it is possible to extend Apache Calcite framework in order to implement indexes and use it during building query execution plan.

3. Where can I use TweeCache?

Answer: Anywhere you wish. For example, make aggregation queries against data stored at cache or maybe for testing purpose.