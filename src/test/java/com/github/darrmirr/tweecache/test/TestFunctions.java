package com.github.darrmirr.tweecache.test;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class TestFunctions {

    public static List<String> string2array(String text, String delimiter) {
        return text != null ? asList(text.split(delimiter)) : new LinkedList<>();
    }

    public static List<Integer> string2arrayInt(String text, String delimiter) {
        return string2int(string2array(text, delimiter));
    }

    public static List<Integer> string2int(List<String> list) {
        if (list == null) {
            return null;
        }
        return list
                .stream()
                .map(item -> item == null ? null : Integer.valueOf(item))
                .collect(Collectors.toList());
    }
}
