package org.zstack.compute.vm.numa;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
public interface VmNumaConstant {
    String RULES_SEPARATOR = ";";
    String V_P_CPU_SEPARATOR = ":";
    String CPU_SET_SEPARATOR = ",";
    String CPU_SET_INVERT_PREFIX = "^";
    int MEMORY_FINENESS = 1024;

    enum ClusterType {
        COMMON_CLUSTER
    }
}
