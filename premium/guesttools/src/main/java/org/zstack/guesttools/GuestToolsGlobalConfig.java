package org.zstack.guesttools;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.storage.memorySnapshot.NeedRestoreOnVmApplySnapshot;

/**
 * Created by shixin on 2025/06/18
 */
@GlobalConfigDefinition
public class GuestToolsGlobalConfig {
    public static final String CATEGORY = "guestTools";

    @NeedRestoreOnVmApplySnapshot
    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig CONFIG_IPADDRESS_WITH_HOSTNAME = new GlobalConfig(CATEGORY, "configure.ip.with.hostname");

    @NeedRestoreOnVmApplySnapshot
    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig PUSH_NETWORK_CONFIG_VIA_QGA = new GlobalConfig(CATEGORY, "push.network.config.via.qga");
}
