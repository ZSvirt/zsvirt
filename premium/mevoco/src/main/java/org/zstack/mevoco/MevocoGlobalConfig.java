package org.zstack.mevoco;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by frank on 9/16/2015.
 */
@GlobalConfigDefinition
public class MevocoGlobalConfig {
    public static final String CATEGORY = "mevoco";

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "1.0", type = Double.class, description = "the over-provisioning ratio of memory, " +
            "which must be greater than 1. For example, 1.2 means to treat the memory as 120% of current size")
    @BindResourceConfig({HostVO.class, ClusterVO.class, ZoneVO.class})
    public static PremiumGlobalConfig MEMORY_OVER_PROVISIONING_RATIO = new PremiumGlobalConfig(CATEGORY, "overProvisioning.memory");
    /*
    @GlobalConfigValidation
    public static PremiumGlobalConfig CPU_OVER_PROVISIONING_RATIO = new PremiumGlobalConfig(CATEGORY, "overProvisioning.cpu");
    */
    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "1.0", type = Double.class, description = "the over-provisioning ratio of primary storage, " +
            "which must be greater than 1. For example, 1.2 means to treat the primary storage as 120% of current size")
    @BindResourceConfig({PrimaryStorageVO.class, ZoneVO.class})
    public static PremiumGlobalConfig PRIMARY_STORAGE_OVER_PROVISIONING_RATIO = new PremiumGlobalConfig(CATEGORY, "overProvisioning.primaryStorage");

    @GlobalConfigValidation
    public static PremiumGlobalConfig HOST_ALLOCATOR_STRATEGY = new PremiumGlobalConfig(CATEGORY, "hostAllocatorStrategy");

    @GlobalConfigValidation
    public static PremiumGlobalConfig DISTRIBUTE_IMAGE = new PremiumGlobalConfig(CATEGORY, "distributeImage");

    @GlobalConfigValidation
    public static PremiumGlobalConfig DISTRIBUTE_IMAGE_PARA_LEVEL = new PremiumGlobalConfig(CATEGORY, "distributeImage.concurrency");

    @GlobalConfigValidation(validValues = {"vnc", "spice", "vncAndSpice"})
    public static PremiumGlobalConfig VM_CONSOLE_MODE = new PremiumGlobalConfig(CATEGORY, "vm.consoleMode");

    @GlobalConfigValidation(min = -1, max = 10)
    public static PremiumGlobalConfig VM_API_RETRY = new PremiumGlobalConfig(CATEGORY, "apiRetry.vm");

    @GlobalConfigValidation(min = 0, max = 3600)
    public static PremiumGlobalConfig VM_API_RETRY_INTERVAL = new PremiumGlobalConfig(CATEGORY, "apiRetry.interval.vm");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "0.9", type = Double.class, description = "the threshold of primary storage physical capacity." +
            "If the physical capacity usage exceeds this value, the primary storage is considered as full, and no volume can be created on it")
    @BindResourceConfig({PrimaryStorageVO.class})
    public static PremiumGlobalConfig PRIMARY_STORAGE_PHYSICAL_CAPACITY_THRESHOLD = new PremiumGlobalConfig(CATEGORY, "threshold.primaryStorage.physicalCapacity");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VolumeVO.class, VmInstanceVO.class})
    public static PremiumGlobalConfig AIO_NATIVE = new PremiumGlobalConfig(CATEGORY, "aio.native");

    @GlobalConfigValidation(inNumberRange = {512, 2097152})
    public static GlobalConfig QCOW2_CLUSTER_SIZE = new PremiumGlobalConfig(CATEGORY, "qcow2.cluster.size");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static PremiumGlobalConfig DELETE_TEMP_IMAGES = new PremiumGlobalConfig(CATEGORY, "deleteTempImages");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig XFS_FRAG_SCRAPE_INTERVAL = new GlobalConfig(CATEGORY, "xfsFragScrape.interval");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig XFS_VOLUME_DETECT_SIZE = new GlobalConfig(CATEGORY, "xfsVolumeDetect.size");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable xfs frag scrape")
    public static GlobalConfig ENABLE_XFS_FRAG_SCRAPE = new GlobalConfig(CATEGORY, "xfsFragScrape.enable");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig LICENSE_SCRAPE_INTERVAL = new GlobalConfig(CATEGORY, "license.scrape.interval");

    @GlobalConfigValidation(inNumberRange = {0,255})
    public static GlobalConfig ENABLE_SPICE_CHANNEL_SUPPORT_TLS = new GlobalConfig(CATEGORY,"enableSpiceChannelSupportTLS");

    @GlobalConfigValidation(inNumberRange = {0,255})
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ENABLE_CGROUP_DEVICE_ACL = new GlobalConfig(CATEGORY,"enableCgroupDeviceAcl");

    @GlobalConfigValidation
    public static GlobalConfig ENABLE_SECURITY_LEVEL = new GlobalConfig(CATEGORY,"enable.security.level");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig PAUSE_THE_WORLD = new GlobalConfig(CATEGORY, "pause.world");

    @GlobalConfigValidation(inNumberRange = {720, 1440})
    @GlobalConfigDef(defaultValue = "1440", type = Long.class, description = "Accumulated hours for UKey to stay inserted")
    public static GlobalConfig LICENSE_ACCUMULATIVE_HOURS = new GlobalConfig(CATEGORY, "license.accu.hours");

    @GlobalConfigValidation(inNumberRange = {0, 720})
    @GlobalConfigDef(defaultValue = "720", type = Long.class, description = "Extra life time in hours after UKey unplugged")
    public static GlobalConfig LICENSE_EXTRA_LIFETIME_HOURS = new GlobalConfig(CATEGORY, "license.extra.hours");

    @GlobalConfigDef(defaultValue = "01111,6-8", description = "vm console password strength check config, the default value is 01111,6-8, which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true")
    @GlobalConfigValidation(notEmpty = false)
    public static PremiumGlobalConfig VM_CONSOLE_PASSWORD_STRENGTH_CHECK_CONFIG = new PremiumGlobalConfig(CATEGORY, "vm.console.password.strength.check.config");

    @GlobalConfigDef(defaultValue = "01111,8-18", description = "vm password strength check config, the default value is 01111,8-18, which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true")
    @GlobalConfigValidation(notEmpty = false)
    public static PremiumGlobalConfig VM_PASSWORD_STRENGTH_CHECK_CONFIG = new PremiumGlobalConfig(CATEGORY, "vm.password.strength.check.config");

    @GlobalConfigDef(defaultValue = "01111,8-18", description = "vm domain password strength check config, the default value is 01111,8-18, which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true")
    @GlobalConfigValidation(notEmpty = false)
    public static PremiumGlobalConfig VM_DOMAIN_PASSWORD_STRENGTH_CHECK_CONFIG = new PremiumGlobalConfig(CATEGORY, "vm.domain.password.strength.check.config");

    @GlobalConfigValidation(validValues = {"NONE", "ISO", "VFD"})
    public static PremiumGlobalConfig AUTO_ATTACH_VIRTIO_DRIVER = new PremiumGlobalConfig(CATEGORY, "vm.windows.auto.attach.virtio.driver.format");

    @GlobalConfigDef(defaultValue = "86400", type = Long.class, description = "The hypervisor version check and alarm interval in seconds")
    @GlobalConfigValidation(inNumberRange = {60, 86400 * 365})
    public static PremiumGlobalConfig HYPERVISOR_VERSION_ALARM_INTERVAL = new PremiumGlobalConfig(CATEGORY, "host.hypervisor.version.alarm.interval.in.seconds");
}
