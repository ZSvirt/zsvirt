package org.zstack.monitoring.items.vm;

import static org.zstack.core.Platform.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/8.
 */
public abstract class VmCpuUtilItem implements VmItem {
    public static final String NAME = "vm.cpu.util";
    public static final String TYPE_USED = "used";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_USED);
    }

    public VmCpuUtilItem() {
    }

    public String getName() {
        return NAME;
    }

    public String getReadableName() {
        return i18n("VM CPU utilization");
    }
}
