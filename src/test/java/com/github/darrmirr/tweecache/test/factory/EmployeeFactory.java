package com.github.darrmirr.tweecache.test.factory;

import com.github.darrmirr.tweecache.test.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class EmployeeFactory {
    private static final Logger log = LoggerFactory.getLogger(EmployeeFactory.class);

    public static Employee createEmployee1() {
        Employee employee = new Employee();
        employee.id = 1;
        employee.firstName = "Ivan";
        employee.middleName = "Ivanovich";
        employee.lastName = "Ivanov";
        return employee;
    }

    public static Employee createEmployee2() {
        Employee employee = new Employee();
        employee.id = 2;
        employee.firstName = "Petr";
        employee.middleName = "Petrovich";
        employee.lastName = "Petrov";
        return employee;
    }

    public static Employee createEmployee3() {
        Employee employee = new Employee();
        employee.id = 3;
        employee.firstName = "Katya";
        employee.middleName = "Ivanovna";
        employee.lastName = "Ivanova";
        return employee;
    }

    public static Function<ResultSet, Employee> employeeRowMapper() {
        return resultSet -> {
            try {
                Employee employee = new Employee();
                employee.id = resultSet.getInt("id");
                employee.firstName = resultSet.getString("firstname");
                employee.middleName = resultSet.getString("middlename");
                employee.lastName = resultSet.getString("lastName");
                return employee;
            } catch (SQLException e) {
                log.error("error to retrieve data from result set", e);
            }
            return null;
        };
    }
}
