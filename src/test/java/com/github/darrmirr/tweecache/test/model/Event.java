package com.github.darrmirr.tweecache.test.model;

import java.util.Date;

public class Event {
    public int id;
    /**
     * Apache Calcite validator evaluates fail if word "date" is present as table column name.
     * It looks like word "date" is reserved one.
     */
    public Date dateTime;
}
