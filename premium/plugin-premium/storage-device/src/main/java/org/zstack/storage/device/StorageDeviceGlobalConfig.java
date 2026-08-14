package org.zstack.storage.device;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Create by weiwang at 2018/8/7
 */
@GlobalConfigDefinition
public class StorageDeviceGlobalConfig {
    public static final String CATEGORY = "storageDevice";

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_FC_DEVICE_SCAN = new PremiumGlobalConfig(CATEGORY, "enable.fc.device.scan");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_NVME_DEVICE_SCAN = new PremiumGlobalConfig(CATEGORY, "enable.nvme.device.scan");

    @BindResourceConfig(value = ClusterVO.class)
    @GlobalConfigValidation(validValues = {"enable", "disable", "ignore"})
    public static GlobalConfig ENABLE_MULTIPATH = new PremiumGlobalConfig(CATEGORY, "enable.multipath");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_LOCALRAID = new PremiumGlobalConfig(CATEGORY, "enable.localraid");

    @GlobalConfigValidation
    public static GlobalConfig ATTACH_TO_CLUSTER_RETRY_TIMES = new PremiumGlobalConfig(CATEGORY, "serverAttachToCluster.retryTimes");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ALLOW_DIFFERENT_ISCSI_CONFIG = new PremiumGlobalConfig(CATEGORY, "allow.differentIscsiConfig");

    @GlobalConfigValidation(notEmpty = false, notNull = false)
    @BindResourceConfig(value = ClusterVO.class)
    public static GlobalConfig MULTIPATH_BLACKLIST = new PremiumGlobalConfig(CATEGORY, "multipath.blacklist");

}
