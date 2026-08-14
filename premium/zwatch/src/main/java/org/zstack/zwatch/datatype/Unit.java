package org.zstack.zwatch.datatype;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

public class Unit {
    private static List<Unit> units = new ArrayList<>();

    private String name;
    private Class javaClass;

    public static final Unit COUNT = new Unit("Count", Long.class);
    public static final Unit PERCENT = new Unit("Percent", Float.class);
    public static final Unit BYTES = new Unit("Bytes", Long.class);
    public static final Unit BYTES_PER_SECOND = new Unit("BytesPerSecond", Long.class);
    public static final Unit COUNT_PER_SECOND = new Unit("CountPerSecond", Long.class);
    public static final Unit STATE = new Unit("State", Long.class);
    public static final Unit TIME_POINT = new Unit("TimePoint", Long.class);
    public static final Unit TIME_DURATION = new Unit("TimeDuration", Long.class);
    public static final Unit TEMPERATURE = new Unit("Temperature", Float.class);


    public Unit(String name, Class clz) {
        this.name = name;
        this.javaClass = clz;

        if (units.contains(this)) {
            throw new CloudRuntimeException(String.format("duplicated unit[%s]", name));
        }

        units.add(this);
    }

    public Class getJavaClass() {
        return javaClass;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return name.equals(o);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
