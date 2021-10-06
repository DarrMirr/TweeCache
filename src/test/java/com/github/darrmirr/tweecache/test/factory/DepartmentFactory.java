package com.github.darrmirr.tweecache.test.factory;

import com.github.darrmirr.tweecache.test.model.Department;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

public class DepartmentFactory {
    private static final Logger log = LoggerFactory.getLogger(DepartmentFactory.class);

    public static Function<ResultSet, Department> departmentRowMapper() {
        return resultSet -> {
            try {
                Department department = new Department();
                department.id = resultSet.getInt("id");
                department.name = resultSet.getString("name");
                return department;
            } catch (SQLException e) {
                log.error("error to retrieve data from result set", e);
            }
            return null;
        };
    }

    public static Department createDepartment1() {
        Department department = new Department();
        department.id = 1;
        department.name = "IT department";
        return department;
    }

    public static Department createDepartment2() {
        Department department = new Department();
        department.id = 2;
        department.name = "Sales department";
        return department;
    }
}
