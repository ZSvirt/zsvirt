package org.zstack.compute.host;

public interface HostResourceAllocationStrategyConstant {
    String CPU_SEPARATOR = ",";
    String NODE_SEPARATOR = ";";
    String NODE_CPU_SEPARATOR = ":";
    String CPU_RANGE_BETWEEN = "-";
    String CPU_RANGE_EXCLUDE = "^";

    String CONTINUOUS_STRATEGY = "continuous";

    String NORMAL_SCENE = "normal";
    String PERFORMANCE_SCENE = "performance";
}
