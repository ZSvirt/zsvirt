package org.zstack.simulator;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class SimulatorGlobalProperty {
    @GlobalProperty(name="Simulator.notCacheAgentCommand", defaultValue = "false")
    public static boolean NOT_CACHE_AGENT_COMMAND;
}
