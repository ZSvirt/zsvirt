package org.zstack.storage.primary.sharedblock;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class SharedBlockGlobalConfig {
    public static final String CATEGORY = "sharedblock";

    @GlobalConfigValidation
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig SHRINK_SNAPSHOT = new GlobalConfig(CATEGORY, "snapshot.shrink");

    @GlobalConfigValidation
    public static GlobalConfig COMPARE_BEFORE_SHRINK = new GlobalConfig(CATEGORY, "snapshot.compare");

    // TODO(WeiW): Support specify pe size, metadata size, metadata copies

    @GlobalConfigValidation(validValues = {"none", "metadata"})
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig QCOW2_ALLOCATION = new PremiumGlobalConfig(CATEGORY, "qcow2.allocation");

    @GlobalConfigValidation(inNumberRange = {512, 2097152})
    public static GlobalConfig QCOW2_CLUSTER_SIZE = new PremiumGlobalConfig(CATEGORY, "qcow2.cluster.size");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig CHECK_IO_FENCER = new PremiumGlobalConfig(CATEGORY, "fencer.check.io");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig DISABLE_HOST_PS_FAILED = new PremiumGlobalConfig(CATEGORY, "disable.host.when.storage.failure");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig FAIL_IF_MULTIPATH_NO_PATH = new PremiumGlobalConfig(CATEGORY, "fail.if.multipath.no.path");

    @GlobalConfigValidation(min = 1073741824)
    public static GlobalConfig THIN_PROVISIONING_INITIALIZE_SIZE = new PremiumGlobalConfig(CATEGORY, "thin.provisioning.initialize.size");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig THIN_PROVISIONING_VOLUME_UTILIZATION_PERCENT = new PremiumGlobalConfig(CATEGORY, "thin.provisioning.volume.utilization.percent");

    @GlobalConfigValidation(min = 1073741824)
    public static GlobalConfig THIN_PROVISIONING_VOLUME_FREESPACE = new PremiumGlobalConfig(CATEGORY, "thin.provisioning.volume.freespace");

    @GlobalConfigValidation(min = 1073741824)
    public static GlobalConfig THIN_PROVISIONING_VOLUME_INCREMENT = new PremiumGlobalConfig(CATEGORY, "thin.provisioning.volume.increment");

    @GlobalConfigValidation(min = 2)
    public static GlobalConfig LOCK_HELPER_MAX_TIMES = new PremiumGlobalConfig(CATEGORY, "lockhelper.max.times");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig LOCK_HELPER_PROTECTION_PERIOD = new PremiumGlobalConfig(CATEGORY, "lockhelper.protection.period");

    @GlobalConfigValidation(min = 5)
    public static GlobalConfig LOCK_HELPER_SCAN_INTERVAL = new PremiumGlobalConfig(CATEGORY, "lockhelper.scan.interval");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VERBOSE_LOG = new PremiumGlobalConfig(CATEGORY, "sblk.agent.verbose.log");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_LVMETAD = new PremiumGlobalConfig(CATEGORY, "enable.lvmetad");

    @GlobalConfigValidation(inNumberRange = {2, 59})
    @BindResourceConfig({ClusterVO.class})
    public static PremiumGlobalConfig HEARTBEAT_IO_TIMEOUT = new PremiumGlobalConfig(CATEGORY, "heartbeat.io.timeout");

    @GlobalConfigValidation(min = 1)
    public static PremiumGlobalConfig MAX_ACTUAL_SIZE_FACTOR = new PremiumGlobalConfig(CATEGORY, "max.actual.size.factor");

    @GlobalConfigValidation(validValues = {"none", "minLvCounts", "maxFreeSize"})
    @BindResourceConfig({PrimaryStorageVO.class})
    public static PremiumGlobalConfig DEVICE_ALLOCATE_STRATEGY = new PremiumGlobalConfig(CATEGORY, "device.allocate.strategy");

    @GlobalConfigValidation(min = 1)
    public static PremiumGlobalConfig VOLUME_INSTANTIATE_SYNC_LEVEL = new PremiumGlobalConfig(CATEGORY, "volume.instantiate.sync.level");

    @GlobalConfigValidation(min = 1)
    public static PremiumGlobalConfig VOLUME_ACTIVATE_SYNC_LEVEL = new PremiumGlobalConfig(CATEGORY, "volume.activate.sync.level");

    @GlobalConfigValidation(validValues = {"always", "never", "auto"})
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig DISCARD_VOLUME_WHEN_DELETING = new PremiumGlobalConfig(CATEGORY, "discard.volume.when.deleting");
}
