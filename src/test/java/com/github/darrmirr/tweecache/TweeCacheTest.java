package com.github.darrmirr.tweecache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.darrmirr.tweecache.test.model.*;
import com.github.darrmirr.tweecache.test.TestFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.darrmirr.tweecache.test.factory.ComputerFactory.*;
import static com.github.darrmirr.tweecache.test.factory.DepartmentFactory.*;
import static com.github.darrmirr.tweecache.test.factory.EmployeeFactory.*;
import static com.github.darrmirr.tweecache.test.factory.EventFactory.*;
import static com.github.darrmirr.tweecache.test.factory.IncidentFactory.*;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Class contains library usage examples as unit tests.
 * Also tests cover some cases of base functionality that library should provide.
 */
@Execution(ExecutionMode.CONCURRENT)
class TweeCacheTest {
    private TweeCache tweeCache;

    @AfterEach
    void tearDown() {
        if(tweeCache != null) {
            tweeCache.destroy();
        }
    }

    @Test
    void createTweeCache() {
        tweeCache = TweeCache
                .builder("cache")
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
        List<Employee> employeeList = tweeCache.query("select emp.* from cache.employee as emp", employeeRowMapper());

        assertEquals(employee.id, employeeList.get(0).id);
        assertEquals(employee.firstName, employeeList.get(0).firstName);
        assertEquals(employee.middleName, employeeList.get(0).middleName);
        assertEquals(employee.lastName, employeeList.get(0).lastName);
    }

    @Test
    public void multiplyCacheCreation() {
        TweeCache tweeCacheEmployee = TweeCache
                .builder("cache1")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);
        TweeCache tweeCacheDepartment = TweeCache
                .builder("cache2")
                .withSchema(builder -> builder
                        .addTable(Department.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);
        Employee employee = createEmployee1();
        tweeCacheEmployee.put(employee.id, employee);

        Department department = createDepartment1();
        tweeCacheDepartment.put(department.id, department);

        List<Employee> employeeList = tweeCacheEmployee.query("select emp.* from cache1.employee as emp", employeeRowMapper());
        List<Department> departmentList = tweeCacheDepartment.query("select dep.* from cache2.department as dep", departmentRowMapper());

        assertEquals(employee.id, employeeList.get(0).id);
        assertEquals(employee.firstName, employeeList.get(0).firstName);
        assertEquals(employee.middleName, employeeList.get(0).middleName);
        assertEquals(employee.lastName, employeeList.get(0).lastName);

        assertEquals(department.id, departmentList.get(0).id);
        assertEquals(department.name, departmentList.get(0).name);
        tweeCacheEmployee.destroy();
        tweeCacheDepartment.destroy();
    }

    @Test
    void scalarFunction() {
        tweeCache = TweeCache
                .builder("cacheSF")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build)
                        .addFunction("string_to_array", TestFunctions.class, "string2array")
                        .addFunction("string_to_array_int", TestFunctions.class, "string2arrayInt"))
                .build()
                .orElseThrow(IllegalStateException::new);
        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        Employee employee3 = createEmployee3();

        tweeCache.put(employee1.id, employee1)
                .put(employee2.id, employee2)
                .put(employee3.id, employee3);

        String sqlQuery = "select emp.* from cachesf.employee as emp where emp.id = any(select * from unnest(string_to_array_int('1,3', ',')))";
        List<Employee> employeeList = tweeCache.query(sqlQuery, employeeRowMapper());

        assertEquals(employeeList.size(), 2);
        employeeList.forEach(employee ->
                assertThat(employee.id, isOneOf(1, 3)));
    }

    @Test
    void namedParameter() {
        tweeCache = TweeCache
                .builder("cacheNP")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);
        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        Employee employee3 = createEmployee3();

        tweeCache.put(employee1.id, employee1)
                .put(employee2.id, employee2)
                .put(employee3.id, employee3);

        String sqlQuery = "select emp.* from cachenp.employee as emp where emp.firstName = :firstName";
        List<Employee> employeeList = tweeCache.query(sqlQuery, singletonMap("firstName", "Katya"), employeeRowMapper());

        assertThat(employeeList, hasSize(1));
        assertThat(employeeList.get(0).firstName, is("Katya"));
    }

    @Test
    void join() {
        tweeCache = TweeCache
                .builder("cachejoin")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build)
                        .addTable(Department.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);
        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        Employee employee3 = createEmployee3();

        Department department1 = createDepartment1();
        Department department2 = createDepartment2();

        employee1.departmentId = department1.id;
        employee2.departmentId = department2.id;
        employee3.departmentId = department1.id;

        tweeCache.put(employee1.id, employee1)
                .put(employee2.id, employee2)
                .put(employee3.id, employee3)
                .put(department1.id, department1)
                .put(department2.id, department2);

        String sqlQuery = "select emp.*  from cachejoin.employee emp " +
                "left join cachejoin.department as dep on emp.departmentId = dep.id " +
                "where dep.name = :dep_name";
        List<Employee> employeeList = tweeCache.query(sqlQuery, singletonMap("dep_name", "IT department"), employeeRowMapper());

        assertThat(employeeList, hasSize(2));
        employeeList.forEach(employee ->
                assertThat(employee.id, isOneOf(1, 3)));
    }

    @Test
    void queryCache() {
        tweeCache = TweeCache
                .builder("calciteQueryCache")
                .withSchema(builder -> builder
                        .addTable(Department.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Department department1 = createDepartment1();

        tweeCache.put(department1.id, department1);

        String sqlQuery = "select dep.* from calciteQueryCache.department as dep";
        long startTimeQuery1 = System.currentTimeMillis();
        List<Department> queryResult1 = tweeCache.query(sqlQuery, departmentRowMapper());
        long query1ExecutionTime = System.currentTimeMillis() - startTimeQuery1;

        long startTimeQuery2 = System.currentTimeMillis();
        List<Department> queryResult2 = tweeCache.query(sqlQuery, departmentRowMapper());
        long query2ExecutionTime = System.currentTimeMillis() - startTimeQuery2;

        assertThat(query1ExecutionTime, greaterThan(query2ExecutionTime));
        assertThat(queryResult1, hasSize(1));
        assertThat(queryResult2, hasSize(1));
        assertThat(queryResult1.get(0).id, is(equalTo(queryResult2.get(0).id)));
        assertThat(queryResult1.get(0).name, is(equalTo(queryResult2.get(0).name)));
    }

    @Test
    void expireAfterWrite() throws InterruptedException {
        int lifeTime = 15;
        tweeCache = TweeCache
                .builder("expireAfterWrite")
                .withSchema(builder -> builder
                        .addTable(Department.class)
                        .withStorage(caffeineBuilder -> caffeineBuilder
                                .softValues()
                                .expireAfterWrite(lifeTime, TimeUnit.SECONDS)
                                .build()
                        ))
                .build()
                .orElseThrow(IllegalStateException::new);

        Department department1 = createDepartment1();
        tweeCache.put(department1.id, department1);
        long writeTime = System.currentTimeMillis();

        String sqlQuery = "select dep.* from expireAfterWrite.department as dep";
        List<Department> queryResult1 = tweeCache.query(sqlQuery, departmentRowMapper());

        long waitTime = TimeUnit.SECONDS.toMillis(lifeTime) - (System.currentTimeMillis() - writeTime);
        TimeUnit.MILLISECONDS.sleep(waitTime);

        List<Department> queryResult2 = tweeCache.query(sqlQuery, departmentRowMapper());

        assertThat(queryResult1, hasSize(1));
        assertThat(queryResult1.get(0).id, is(1));
        assertThat(queryResult2, hasSize(0));
    }

    @Test
    void getAll() {
        tweeCache = TweeCache
                .builder("cacheGetAll")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        Employee employee3 = createEmployee3();
        Map<Integer, Employee> employeeMap = new HashMap<>();
        employeeMap.put(employee1.id, employee1);
        employeeMap.put(employee2.id, employee2);
        employeeMap.put(employee3.id, employee3);

        tweeCache.putAll(employeeMap);

        List<Employee> employeeList = tweeCache.getAll(Employee.class);

        assertThat(employeeList, hasSize(3));
        employeeList.forEach(employee -> assertThat(employee.id, isOneOf(1, 2, 3)));
    }

    @Test
    void getById() {
        tweeCache = TweeCache
                .builder("cacheGetById")
                .withSchema(builder -> builder
                        .addTable(Employee.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Employee employee1 = createEmployee1();

        tweeCache.put(employee1.id, employee1);
        Optional<Employee> employeeOptional = tweeCache.getById(employee1.id, Employee.class);

        assertThat(employeeOptional.isPresent(), is(true));
        assertThat(employeeOptional.get().id, is(employee1.id));
        assertThat(employeeOptional.get().firstName, is(employee1.firstName));
        assertThat(employeeOptional.get().lastName, is(employee1.lastName));
        assertThat(employeeOptional.get().middleName, is(employee1.middleName));
    }

    @Test
    void queryComplexObject() {
        tweeCache = TweeCache
                .builder("cacheQueryComplexObject")
                .withSchema(builder -> builder
                        .addTable(Computer.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Computer computer1 = computer1();
        Computer computer2 = computer2();

        tweeCache.put(computer1.id, computer1)
                .put(computer2.id, computer2);

        String query = "select comp.*, comp.cpu.name as cpu_name from cacheQueryComplexObject.computer comp where comp.cpu.name = :cpu_name";
        List<Computer> computers = tweeCache.query(query, singletonMap("cpu_name", "ELBRUS"), computerRowMapper());

        assertThat(computers, hasSize(1));
        assertThat(computers.get(0).id, is(1));
        assertThat(computers.get(0).cpu.name, is("ELBRUS"));
        assertThat(computers.get(0).memory, is(32));
    }

    @Test
    void queryWithDate() {
        tweeCache = TweeCache
                .builder("cacheQueryWithDate")
                .withSchema(builder -> builder
                        .addTable(Event.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Event event1 = event1();
        Event event2 = event2();

        tweeCache.put(event1.id, event1)
                .put(event2.id, event2);

        String query = "select evt.* from cacheQueryWithDate.event evt where evt.dateTime = '2021-09-21 00:00:00'";
        List<Event> events = tweeCache.query(query, eventRowMapper());

        assertThat(events, hasSize(1));
        assertThat(events.get(0).id, is(event2.id));
        assertThat(events.get(0).dateTime, is(event2.dateTime));
    }

    @Test
    void queryWithDateParameterString() {
        tweeCache = TweeCache
                .builder("cacheQueryWithDateParameterString")
                .withSchema(builder -> builder
                        .addTable(Event.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Event event1 = event1();
        Event event2 = event2();

        tweeCache.put(event1.id, event1)
                .put(event2.id, event2);

        String query = "select evt.* from cacheQueryWithDateParameterString.event evt where evt.dateTime = cast(:event_date as varchar)";
        List<Event> events = tweeCache.query(query, singletonMap("event_date", "2021-09-21 00:00:00"), eventRowMapper());

        assertThat(events, hasSize(1));
        assertThat(events.get(0).id, is(event2.id));
        assertThat(events.get(0).dateTime, is(event2.dateTime));
    }

    @Test
    void queryWithDateParameterDate() {
        tweeCache = TweeCache
                .builder("cacheQueryWithDateParameterDate")
                .withSchema(builder -> builder
                        .addTable(Event.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Event event1 = event1();
        Event event2 = event2();

        tweeCache.put(event1.id, event1)
                .put(event2.id, event2);

        String query = "select evt.* from cacheQueryWithDateParameterDate.event evt where evt.dateTime = cast(:event_date as timestamp)";
        List<Event> events = tweeCache.query(query, singletonMap("event_date", event2.dateTime), eventRowMapper());

        assertThat(events, hasSize(1));
        assertThat(events.get(0).id, is(event2.id));
        assertThat(events.get(0).dateTime, is(event2.dateTime));
    }

    @Test
    void queryWithArray() {
        tweeCache = TweeCache
                .builder("cacheQueryWithArray")
                .withSchema(builder -> builder
                        .addTable(Incident.class)
                        .withStorage(Caffeine::build))
                .build()
                .orElseThrow(IllegalStateException::new);

        Incident incident1 = incident1();
        Incident incident2 = incident2();

        tweeCache.put(incident1.id, incident1)
                .put(incident2.id, incident2);

        String query = "select inc.* from cacheQueryWithArray.incident inc where :service_name = any(select * from unnest(inc.servicesInvolved))";
        List<Incident> incidents = tweeCache.query(query, singletonMap("service_name", SERVICE_02), incidentRowMapper());

        assertThat(incidents, hasSize(1));
        assertThat(incidents.get(0).id, is(incident2.id));
        assertThat(incidents.get(0).servicesInvolved, is(incident2.servicesInvolved));
    }

    @Test
    void queryWithTheSameTableClass() {
        tweeCache = TweeCache
                .builder("cacheQueryWithTheSameTableClass")
                .withSchema(schemaBuilder ->
                        schemaBuilder
                                .addTable(Employee.class, "employee_from_1_to_99")
                                .withStorage(Caffeine::build)
                                .addTable(Employee.class, "employee_from_100_to_199")
                                .withStorage(Caffeine::build)
                )
                .build()
                .orElseThrow(IllegalStateException::new);

        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        employee2.id = 100;

        tweeCache
                .put("employee_from_1_to_99", employee1.id, employee1)
                .put("employee_from_100_to_199", employee2.id, employee2);

        String query1 = "select emp.* from cacheQueryWithTheSameTableClass.employee_from_1_to_99 emp";
        String query2 = "select emp.* from cacheQueryWithTheSameTableClass.employee_from_100_to_199 emp";

        List<Employee> employeeList1 = tweeCache.query(query1, employeeRowMapper());
        List<Employee> employeeList2 = tweeCache.query(query2, employeeRowMapper());

        assertThat(employeeList1, hasSize(1));
        assertThat(employeeList1.get(0).id, is(1));
        assertThat(employeeList2, hasSize(1));
        assertThat(employeeList2.get(0).id, is(100));
    }

    @Test
    void withTheSameTableClass() {
        tweeCache = TweeCache
                .builder("cacheWithTheSameTableClass")
                .withSchema(schemaBuilder ->
                        schemaBuilder
                                .addTable(Employee.class, "employee_from_1_to_99")
                                .withStorage(Caffeine::build)
                                .addTable(Employee.class, "employee_from_100_to_199")
                                .withStorage(Caffeine::build)
                )
                .build()
                .orElseThrow(IllegalStateException::new);

        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        employee2.id = 100;

        tweeCache
                .put("employee_from_1_to_99", employee1.id, employee1)
                .put("employee_from_100_to_199", employee2.id, employee2);

        Optional<Employee> employee1Optional = tweeCache.getById("employee_from_1_to_99", employee1.id, Employee.class);
        Optional<Employee> employee2Optional = tweeCache.getById("employee_from_1_to_99", employee2.id, Employee.class);
        Optional<Employee> employee3Optional = tweeCache.getById("employee_from_100_to_199", employee1.id, Employee.class);
        Optional<Employee> employee4Optional = tweeCache.getById("employee_from_100_to_199", employee2.id, Employee.class);

        assertThat(employee1Optional.isPresent(), is(true));
        assertThat(employee1Optional.get().id, is(1));
        assertThat(employee2Optional.isPresent(), is(false));
        assertThat(employee3Optional.isPresent(), is(false));
        assertThat(employee4Optional.isPresent(), is(true));
        assertThat(employee4Optional.get().id, is(100));
    }

    @Test
    void withTheSameTableClassPutAll() {
        tweeCache = TweeCache
                .builder("cacheWithTheSameTableClassPutAll")
                .withSchema(schemaBuilder ->
                        schemaBuilder
                                .addTable(Employee.class, "employee_from_1_to_99")
                                .withStorage(Caffeine::build)
                                .addTable(Employee.class, "employee_from_100_to_199")
                                .withStorage(Caffeine::build)
                )
                .build()
                .orElseThrow(IllegalStateException::new);

        Employee employee1 = createEmployee1();
        Employee employee2 = createEmployee2();
        Employee employee3 = createEmployee3();
        employee3.id = 100;

        Map<Integer, Employee> employeeMap = new HashMap<>();
        employeeMap.put(employee1.id, employee1);
        employeeMap.put(employee2.id, employee2);

        tweeCache
                .putAll("employee_from_1_to_99", employeeMap)
                .put("employee_from_100_to_199", employee3.id, employee3);

        List<Employee> employeeList = tweeCache.getAll("employee_from_1_to_99");


        assertThat(employeeList, hasSize(2));
        employeeList.forEach(employee ->
                assertThat(employee.id, isOneOf(1, 2)));
    }
}