package com.github.darrmirr.tweecache.test.model;

import org.apache.calcite.adapter.java.Array;

import java.util.List;

public class Incident {
    public Integer id;
    @Array(component = String.class)
    public List<String> servicesInvolved;

    public Incident() {
    }

    public Incident(Integer id, List<String> servicesInvolved) {
        this.id = id;
        this.servicesInvolved = servicesInvolved;
    }

    public Integer getId() {
        return id;
    }

    public List<String> getServicesInvolved() {
        return servicesInvolved;
    }
}
