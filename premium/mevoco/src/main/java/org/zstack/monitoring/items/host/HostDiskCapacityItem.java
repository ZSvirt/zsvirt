package org.zstack.monitoring.items.host;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lining on 2017/9/14.
 */
public abstract class HostDiskCapacityItem implements HostItem {
    public static final String NAME = "host.disk.capacity";
    public static final String TYPE_AVAIL_SIZE = "avail_size";
    public static final String TYPE_AVAIL_PERCENT = "avail_percent";

    /**
     * 1.multiple disks, separated by "|" , for example: "/dev/vda1|/dev/vdb1|/dev/vdb2"
     * 2.If not specified, Check the physical machine's available storage capacity
     */
    public static final String DEVICES_KEY = "devices";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_AVAIL_SIZE);
        ALLOWED_TYPES.add(TYPE_AVAIL_PERCENT);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getReadableName() {
        return "Host Disk CAPACITY";
    }
}
