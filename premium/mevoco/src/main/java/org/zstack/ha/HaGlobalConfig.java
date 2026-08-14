package org.zstack.ha;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by xing5 on 2016/3/28.
 */
@GlobalConfigDefinition
public class HaGlobalConfig {
    public static final String CATEGORY = "ha";

    @GlobalConfigValidation
    public static PremiumGlobalConfig ALL = new PremiumGlobalConfig(CATEGORY, "enable");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_CHECK_INTERVAL = new PremiumGlobalConfig(CATEGORY, "host.check.interval");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_CHECK_MAX_ATTEMPTS = new PremiumGlobalConfig(CATEGORY, "host.check.maxAttempts");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_CHECK_SUCCESS_INTERVAL = new PremiumGlobalConfig(CATEGORY, "host.check.successInterval");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_CHECK_SUCCESS_TIMES = new PremiumGlobalConfig(CATEGORY, "host.check.successTimes");
    @GlobalConfigValidation
    public static PremiumGlobalConfig CHECK_SUCCESS_RATIO = new PremiumGlobalConfig(CATEGORY, "host.check.successRatio");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_SELF_FENCER_INTERVAL = new PremiumGlobalConfig(CATEGORY, "host.selfFencer.interval");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig HOST_SELF_FENCER_ATTEMPTS = new PremiumGlobalConfig(CATEGORY, "host.selfFencer.maxAttempts");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig NEVER_STOP_VM_FAILURE_RETRY_DELAY = new PremiumGlobalConfig(CATEGORY, "neverStopVm.retry.delay");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig STORAGE_CHECKER_TIMEOUT = new PremiumGlobalConfig(CATEGORY, "host.selfFencer.storageChecker.timeout");
    @GlobalConfigValidation(min = 1)
    public static PremiumGlobalConfig NEVER_STOP_VM_SCAN_INTERVAL = new PremiumGlobalConfig(CATEGORY, "neverStopVm.scan.interval");
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig NEVER_STOP_VM_NOTIFICATION_RETRY_TIMES = new PremiumGlobalConfig(CATEGORY, "neverStopVm.notification.times");
    @GlobalConfigValidation
    public static PremiumGlobalConfig SUPPORT_HA_SLIBING_CROSS_CLUSTERS = new PremiumGlobalConfig(CATEGORY, "allow.slibing.cross.clusters");

    // seconds
    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig NEVER_STOP_VM_GC_MAX_RETRY_INTERVAL_TIME = new PremiumGlobalConfig(CATEGORY, "neverStopVm.gc.maxRetryIntervalTime");


    @GlobalConfigValidation(validValues = {"Force", "Permissive"})
    @BindResourceConfig(PrimaryStorageVO.class)
    public static PremiumGlobalConfig SELF_FENCER_STRATEGY = new PremiumGlobalConfig(CATEGORY, "self.fencer.strategy");

    @GlobalConfigValidation(validValues = {"Force", "Permissive"})
    @BindResourceConfig(VmInstanceVO.class)
    public static PremiumGlobalConfig VM_HA_STRATEGY = new PremiumGlobalConfig(CATEGORY, "vm.ha.strategy");

    @GlobalConfigValidation(validValues = {"NeverStop", "OnHostFailure", "FaultTolerance", "None"})
    @BindResourceConfig(ClusterVO.class)
    public static PremiumGlobalConfig VM_HA_LEVEL = new PremiumGlobalConfig(CATEGORY, "vm.ha.level");

    @GlobalConfigValidation(inNumberRange = {-1, 5})
    public static PremiumGlobalConfig HA_NOTIFICATION_TIMELINESS = new PremiumGlobalConfig(CATEGORY, "notification.timeliness");

}
