package org.zstack.utils;

/**
 */
public class Mutable {
    private Object value;

    public <T> T getValue() {
        return (T)value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
