package org.zstack.compute.cluster;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * @ Author : yh.w
 * @ Date   : Created in 10:27 2019/4/29
 */
@GlobalConfigDefinition
public class MevocoClusterGlobalConfig {

    public static final String CATEGORY = "premiumCluster";

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable host hugepage")
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig HUGEPAGE_ENABLE = new GlobalConfig(CATEGORY, "hugepage.enable");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "2", type = Integer.class, description = "hugepage size, in MiB")
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig HUGEPAGE_SIZE = new GlobalConfig(CATEGORY, "hugepage.size");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "Whether to enable zero copy on host")
    @BindResourceConfig(value = {HostVO.class, ClusterVO.class})
    public static GlobalConfig HOST_ENABLE_ZERO_COPY = new GlobalConfig(CATEGORY, "enable.zeroCopy");

    @GlobalConfigValidation(validValues = {"private", "shared"})
    @GlobalConfigDef(defaultValue = "private", type = String.class, description = "set memAccess to the vm belonging to the cluster")
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig MEMORY_ACCESS = new GlobalConfig(CATEGORY, "memAccess.mode");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable ovs dpdk support for cluster")
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig OVS_DPDK_SUPPORT = new GlobalConfig(CATEGORY, "network.ovsdpdk");
}
