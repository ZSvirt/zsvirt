package org.zstack.monitoring.items.host;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/19.
 */
public abstract class HostDiskIOItem implements HostItem {
    public static final String NAME = "host.disk.io";
    public static final String TYPE_IOPS = "iops";
    public static final String TYPE_BANDWIDTH = "bandwidth";
    public static final String DIRECTION_READ = "read";
    public static final String DIRECTION_WRITE = "write";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();
    public static final Set<String> ALLOWED_DIRECTION = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_BANDWIDTH);
        ALLOWED_TYPES.add(TYPE_IOPS);

        ALLOWED_DIRECTION.add(DIRECTION_READ);
        ALLOWED_DIRECTION.add(DIRECTION_WRITE);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getReadableName() {
        return "Host Disk IO";
    }
}
