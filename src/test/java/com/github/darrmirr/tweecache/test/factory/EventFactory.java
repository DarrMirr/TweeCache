package com.github.darrmirr.tweecache.test.factory;

import com.github.darrmirr.tweecache.test.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Function;

public class EventFactory {
    private static final Logger log = LoggerFactory.getLogger(EventFactory.class);
    private static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    public static Event event1() {
        Event event = new Event();
        event.id = 1;
        event.dateTime = Date
                .from(LocalDate.of(2021, 9, 1)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        return event;
    }

    public static Event event2() {
        Event event = new Event();
        event.id = 2;
        event.dateTime = Date
                .from(LocalDate.of(2021, 9, 21)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        return event;
    }

    public static Function<ResultSet, Event> eventRowMapper() {
        return resultSet -> {
            try {
                Event event = new Event();
                event.id = resultSet.getInt("id");
                event.dateTime = Date.from(resultSet.getTimestamp("dateTime", calendar).toInstant());
                return event;
            } catch (SQLException e) {
                log.error("error to retrieve data from result set", e);
            }
            return null;
        };
    }
}
