package com.github.darrmirr.tweecache.test.factory;

import com.github.darrmirr.tweecache.test.model.Computer;
import com.github.darrmirr.tweecache.test.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.function.Function;

public class ComputerFactory {
    private static final Logger log = LoggerFactory.getLogger(ComputerFactory.class);

    public static Computer computer1() {
        Computer computer = new Computer();
        computer.id = 1;
        computer.cpu = new Computer.CPU();
        computer.cpu.name = "ELBRUS";
        computer.memory = 32;
        return computer;
    }

    public static Computer computer2() {
        Computer computer = new Computer();
        computer.id = 2;
        computer.cpu = new Computer.CPU();
        computer.cpu.name = "AMD";
        computer.memory = 16;
        return computer;
    }

    public static Function<ResultSet, Computer> computerRowMapper() {
        return resultSet -> {
            try {
                Computer computer = new Computer();
                computer.id = resultSet.getInt("id");
                computer.memory = resultSet.getInt("memory");
                computer.cpu = new Computer.CPU();
                computer.cpu.name = resultSet.getString("cpu_name");
                return computer;
            } catch (SQLException e) {
                log.error("error to retrieve data from result set", e);
            }
            return null;
        };
    }
}
