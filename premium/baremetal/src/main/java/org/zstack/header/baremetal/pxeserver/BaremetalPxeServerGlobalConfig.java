package org.zstack.header.baremetal.pxeserver;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/**
 * Created by GuoYi on 2018-10-15.
 */
@GlobalConfigDefinition
public class BaremetalPxeServerGlobalConfig {
    public static final String CATEGORY = "baremetalPxeServer";

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig PING_INTERVAL = new GlobalConfig(CATEGORY, "ping.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig PING_PARALLELISM_DEGREE = new GlobalConfig(CATEGORY, "ping.parallelismDegree");

    @GlobalConfigValidation
    public static GlobalConfig RESERVED_CAPACITY = new GlobalConfig(CATEGORY, "reservedCapacity");

    @GlobalConfigValidation
    public static GlobalConfig PXESERVER_ALLOW_PORTS = new GlobalConfig(CATEGORY, "pxeserver.allow.ports");
}
