package org.zstack.compute.ovs;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by haibiao.xiao on 5/24/2022
 */
@GlobalConfigDefinition
public class OvsGlobalConfig {
    public static final String CATEGORY = "ovs";

    @GlobalConfigDef(defaultValue = "", type = String.class, description = "set pmd pinning for ovs-dpdk")
    @BindResourceConfig(value = {HostVO.class, ClusterVO.class})
    public static GlobalConfig OVS_CPU_PINNING = new GlobalConfig(CATEGORY, "ovsdpdk.pmdpinning");

    @GlobalConfigValidation
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    @GlobalConfigDef(defaultValue = "8G", type = String.class, description = "reserve memory for ovs-dpdk, in GiB")
    public static GlobalConfig RESERVED_MEMORY_CAPACITY_FOR_OVSDPDK = new GlobalConfig(CATEGORY, "reservedMemory");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "2", type = Integer.class, description = "hugepage size for ovs dpdk, in MiB")
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig HUGEPAGE_SIZE_FOR_OVSDPDK = new GlobalConfig(CATEGORY, "hugepage.size");

}
