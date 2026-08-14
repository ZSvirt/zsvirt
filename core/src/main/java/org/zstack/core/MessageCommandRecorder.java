package org.zstack.core;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/6/23.
 */
public class MessageCommandRecorder {
    private static Class starter;
    private static boolean enabled;

    private static List<Class> followers = new ArrayList<>();

    static boolean needRun() {
        return CoreGlobalProperty.UNIT_TEST_ON && enabled;
    }

    public static void start(Class s) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        enabled = true;

        if (starter != null) {
            throw new RuntimeException(String.format("already a starter[%s] here", starter));
        }

        starter = s;
    }

    public static synchronized void record(String c) {
        if (!needRun()) {
            return;
        }

        try {
            followers.add(Class.forName(c));
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public static synchronized void record(Class c) {
        if (!needRun()) {
            return;
        }

        followers.add(c);
    }

    public static synchronized List<Class> end() {
        if (!needRun()) {
            return null;
        }

        List<Class> ret = followers.stream().collect(Collectors.toList());
        reset();
        return ret;
    }

    public static synchronized void reset() {
        if (!needRun()) {
            return;
        }

        enabled = false;
        starter = null;
        followers = new ArrayList<>();
    }

    public static synchronized String endAndToString() {
        if (!needRun()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(String.format("\n\nCALLING CHAIN FOR: %s", starter));
        for (Class c : followers) {
            sb.append(String.format("\n ---> %s", c));
        }

        reset();
        return sb.toString();
    }
}
