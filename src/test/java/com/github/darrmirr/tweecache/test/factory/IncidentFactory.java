package com.github.darrmirr.tweecache.test.factory;

import com.github.darrmirr.tweecache.test.model.Event;
import com.github.darrmirr.tweecache.test.model.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class IncidentFactory {
    private static final Logger log = LoggerFactory.getLogger(IncidentFactory.class);
    public static final String SERVICE_01 = "01";
    public static final String SERVICE_02 = "02";
    public static final String SERVICE_03 = "03";

    public static Incident incident1() {
        Incident incident = new Incident();
        incident.id = 1;
        incident.servicesInvolved = Collections.singletonList(SERVICE_01);
        return incident;
    }

    public static Incident incident2() {
        Incident incident = new Incident();
        incident.id = 2;
        incident.servicesInvolved = Arrays.asList(SERVICE_02, SERVICE_03);
        return incident;
    }

    public static Function<ResultSet, Incident> incidentRowMapper() {
        return resultSet -> {
            try {
                Incident incident = new Incident();
                incident.id = resultSet.getInt("id");
                Object o = resultSet.getObject("servicesInvolved");
                if (o instanceof List) {
                    incident.servicesInvolved = (List<String>) o;
                }
                return incident;
            } catch (SQLException e) {
                log.error("error to retrieve data from result set", e);
            }
            return null;
        };
    }
}
