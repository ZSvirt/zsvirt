package org.zstack.monitoring.items.vm;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/19.
 */
public abstract class VmMemUtilItem implements VmItem {
    public static final String NAME = "vm.mem.util";
    public static final String TYPE_FREE = "free";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_FREE);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getReadableName() {
        return "VM Memory Utilization";
    }
}
