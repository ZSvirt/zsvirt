package org.zstack.ha.header;

import org.zstack.core.config.BindResourceConfig;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by xing5 on 2016/3/28.
 */
@GlobalConfigDefinition
public class HaGlobalConfig {
    public static final String CATEGORY = "ha";

    @GlobalConfigValidation
    public static GlobalConfig ALL = new GlobalConfig(CATEGORY, "enable");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_CHECK_INTERVAL = new GlobalConfig(CATEGORY, "host.check.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_CHECK_MAX_ATTEMPTS = new GlobalConfig(CATEGORY, "host.check.maxAttempts");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_CHECK_SUCCESS_INTERVAL = new GlobalConfig(CATEGORY, "host.check.successInterval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_CHECK_SUCCESS_TIMES = new GlobalConfig(CATEGORY, "host.check.successTimes");
    @GlobalConfigValidation
    public static GlobalConfig CHECK_SUCCESS_RATIO = new GlobalConfig(CATEGORY, "host.check.successRatio");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_SELF_FENCER_INTERVAL = new GlobalConfig(CATEGORY, "host.selfFencer.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig HOST_SELF_FENCER_ATTEMPTS = new GlobalConfig(CATEGORY, "host.selfFencer.maxAttempts");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig NEVER_STOP_VM_FAILURE_RETRY_DELAY = new GlobalConfig(CATEGORY, "neverStopVm.retry.delay");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig STORAGE_CHECKER_TIMEOUT = new GlobalConfig(CATEGORY, "host.selfFencer.storageChecker.timeout");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig NEVER_STOP_VM_SCAN_INTERVAL = new GlobalConfig(CATEGORY, "neverStopVm.scan.interval");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig NEVER_STOP_VM_NOTIFICATION_RETRY_TIMES = new GlobalConfig(CATEGORY, "neverStopVm.notification.times");
    @GlobalConfigValidation
    public static GlobalConfig SUPPORT_HA_SLIBING_CROSS_CLUSTERS = new GlobalConfig(CATEGORY, "allow.slibing.cross.clusters");

    // seconds
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig NEVER_STOP_VM_GC_MAX_RETRY_INTERVAL_TIME = new GlobalConfig(CATEGORY, "neverStopVm.gc.maxRetryIntervalTime");


    @GlobalConfigValidation(validValues = {"Force", "Permissive"})
    @BindResourceConfig(PrimaryStorageVO.class)
    public static GlobalConfig SELF_FENCER_STRATEGY = new GlobalConfig(CATEGORY, "self.fencer.strategy");

    @GlobalConfigValidation(validValues = {"Force", "Permissive"})
    @BindResourceConfig(VmInstanceVO.class)
    public static GlobalConfig VM_HA_STRATEGY = new GlobalConfig(CATEGORY, "vm.ha.strategy");

    @GlobalConfigValidation(validValues = {"NeverStop", "OnHostFailure", "None"})
    @BindResourceConfig(ClusterVO.class)
    public static GlobalConfig VM_HA_LEVEL = new GlobalConfig(CATEGORY, "vm.ha.level");

    @GlobalConfigValidation(inNumberRange = {-1, 5})
    public static GlobalConfig HA_NOTIFICATION_TIMELINESS = new GlobalConfig(CATEGORY, "notification.timeliness");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig HA_SYNC_SELF_FENCER_INTERVAL = new GlobalConfig(CATEGORY, "selfFencer.sync.interval");

}
