package org.zstack.testlib.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadFacadeConfigLoader {
    public static final String BLOCKED_PERIODIC_TASK_LIST =
            "UTCapabilities/thread-facade-configs/blocked-periodic-task.list";

    public List<String> loadBlockedPeriodicTasks() {
        InputStream in = ThreadFacadeConfigLoader.class.getClassLoader().getResourceAsStream(BLOCKED_PERIODIC_TASK_LIST);
        if (in == null) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim();

                if (name.isEmpty() || name.startsWith("#")) {
                    continue;
                }

                names.add(name);
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + BLOCKED_PERIODIC_TASK_LIST, e);
        }
        return names;
    }
}
