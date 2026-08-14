package org.zstack.monitoring.items.host;

import static org.zstack.core.Platform.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/8.
 */
public abstract class HostCpuUtilItem implements HostItem {
    public static final String NAME = "host.cpu.util";
    public static final String TYPE_IDLE = "idle";
    public static final String TYPE_USED = "used";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_USED);
        ALLOWED_TYPES.add(TYPE_IDLE);
    }

    public HostCpuUtilItem() {
    }

    public String getName() {
        return NAME;
    }

    public String getReadableName() {
        return i18n("Host CPU utilization");
    }
}
