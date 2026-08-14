package org.zstack.utils;

/**
 */
public class Bucket {
    private Object[] objects;

    public Bucket(Object...objs) {
        objects = objs;
    }

    public static Bucket newBucket(Object...objs) {
        return new Bucket(objs);
    }

    public <T> T get(int index) {
        if (index < 0 || index >= objects.length) {
            throw new IllegalArgumentException(String.format("illegal index[%s], bucket size is :%s", index, objects.length));
        }

        return (T) objects[index];
    }

    public <T> T safeGet(int index) {
        if (index < 0 || index >= objects.length) {
            return null;
        }

        return get(index);
    }
}
