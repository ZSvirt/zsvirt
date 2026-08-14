package org.zstack.monitoring.items.vm;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xing5 on 2017/6/19.
 */
public abstract class VmNetworkIOItem implements VmItem {
    public static final String NAME = "vm.network.io";
    public static final String TYPE_BANDWIDTH = "bandwidth";
    public static final String DIRECTION_TX = "tx";
    public static final String DIRECTION_RX = "rx";

    public static final Set<String> ALLOWED_TYPES = new HashSet<>();
    public static final Set<String> ALLOWED_DIRECTION = new HashSet<>();

    static {
        ALLOWED_TYPES.add(TYPE_BANDWIDTH);

        ALLOWED_DIRECTION.add(DIRECTION_TX);
        ALLOWED_DIRECTION.add(DIRECTION_RX);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getReadableName() {
        return "VM Network IO";
    }
}
