package org.zstack.compute.vm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;

@GlobalConfigDefinition
public class VmGlobalConfig {
    public static final String CATEGORY = "vm";

    @GlobalConfigValidation
    public static GlobalConfig DELETE_DATA_VOLUME_ON_VM_DESTROY = new GlobalConfig(CATEGORY, "dataVolume.deleteOnVmDestroy");
    @GlobalConfigValidation
    public static GlobalConfig UPDATE_INSTANCE_OFFERING_TO_NULL_WHEN_DELETING = new GlobalConfig(CATEGORY, "instanceOffering.setNullWhenDeleting");
    @GlobalConfigValidation(validValues = {"Direct","Delay", "Never"})
    public static GlobalConfig VM_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");
    @GlobalConfigValidation(min = 1)
    public static GlobalConfig VM_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VM_CLEAN_TRAFFIC = new GlobalConfig(CATEGORY, "cleanTraffic");
    @GlobalConfigValidation(validValues = {"cirrus","vga", "qxl", "virtio"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_VIDEO_TYPE = new GlobalConfig(CATEGORY, "videoType");
    @GlobalConfigValidation(validValues = {"ich6","ich9", "ac97"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_SOUND_TYPE = new GlobalConfig(CATEGORY, "soundType");
    @GlobalConfigValidation(validValues = {"off","all", "filter"})
    @BindResourceConfig(value = {VmInstanceVO.class})
    public static GlobalConfig VM_SPICE_STREAMING_MODE= new GlobalConfig(CATEGORY, "spiceStreamingMode");
    @GlobalConfigValidation
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig NUMA = new GlobalConfig(CATEGORY, "numa");
    @GlobalConfigValidation
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_MAX_VCPU = new GlobalConfig(CATEGORY, "vm.max.vcpu");
    @GlobalConfigValidation
    public static GlobalConfig VM_BOOT_MENU = new GlobalConfig(CATEGORY, "bootMenu");
    @GlobalConfigValidation(min = 0, max = 65535)
    @BindResourceConfig(value = {VmInstanceVO.class})
    public static GlobalConfig VM_BOOT_MENU_SPLASH_TIMEOUT = new GlobalConfig(CATEGORY, "bootMenuSplashTimeout");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig KVM_HIDDEN_STATE = new GlobalConfig(CATEGORY, "kvmHiddenState");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class})
    public static GlobalConfig VM_PORT_OFF = new GlobalConfig(CATEGORY, "vmPortOff");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig EMULATE_HYPERV = new GlobalConfig(CATEGORY, "emulateHyperV");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ADDITIONAL_QMP = new GlobalConfig(CATEGORY, "additionalQmp");

    @GlobalConfigValidation(validValues = {"true","false"})
    public static GlobalConfig MULTI_VNIC_SUPPORT = new GlobalConfig(CATEGORY, "multivNic.support");

    @GlobalConfigValidation(min = 0, max = VmInstanceConstant.MAXIMUM_CDROM_NUMBER)
    public static GlobalConfig VM_DEFAULT_CD_ROM_NUM = new GlobalConfig(CATEGORY, "vmDefaultCdRomNum");

    @GlobalConfigValidation(min = 1, max = VmInstanceConstant.MAXIMUM_CDROM_NUMBER)
    public static GlobalConfig MAXIMUM_CD_ROM_NUM = new GlobalConfig(CATEGORY, "maximumCdRomNum");

    @GlobalConfigValidation(inNumberRange = {0, 28})
    public static GlobalConfig PCIE_PORT_NUMS = new GlobalConfig(CATEGORY, "pciePortNums");

    @GlobalConfigValidation(validValues = {"Hard", "Soft"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig RESOURCE_BINDING_STRATEGY = new GlobalConfig(CATEGORY, "resourceBinding.strategy");

    @GlobalConfigValidation(validValues = {"None", "Preserve","Reboot","Shutdown"})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_CRASH_STRATEGY = new GlobalConfig(CATEGORY, "crash.strategy");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_REBOOT_THRESHOLD_DURATION = new GlobalConfig(CATEGORY, "crash.rebootThreshold.duration");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_REBOOT_THRESHOLD_TIMES = new GlobalConfig(CATEGORY, "crash.rebootThreshold.times");

    @GlobalConfigValidation(validValues = {"Auto", "All"})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig RESOURCE_BINDING_SCENE = new GlobalConfig(CATEGORY, "resourceBinding.Scene");

    @GlobalConfigValidation(inNumberRange = {1, 256})
    @BindResourceConfig({VmNicVO.class, VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_NIC_MULTIQUEUE_NUM = new GlobalConfig(CATEGORY, "nicMultiQueueNum");

    @GlobalConfigValidation(min = 1)
    public static GlobalConfig UNKNOWN_GC_INTERVAL = new GlobalConfig(CATEGORY, "set.unknown.gc.interval");

    @GlobalConfigDef(defaultValue = "Microsoft Hv", type = String.class, description = "set vendor_id")
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VENDOR_ID = new GlobalConfig(CATEGORY, "vendorId");

    @BindResourceConfig(value = {VmInstanceVO.class})
    @GlobalConfigValidation(validValues = {"guest", "host"})
    public static GlobalConfig VM_CLOCK_TRACK = new GlobalConfig(CATEGORY, "vm.clock.track");

    @BindResourceConfig(value = {VmInstanceVO.class})
    @GlobalConfigValidation(validValues = {"0", "60", "600", "1800", "3600", "7200", "21600", "43200", "86400"})
    @GlobalConfigDef(defaultValue = "0", type = Integer.class, description = "vm clock sync interval in seconds")
    public static GlobalConfig VM_CLOCK_SYNC_INTERVAL_IN_SECONDS = new GlobalConfig(CATEGORY, "vm.clock.sync.interval.in.seconds");

    @BindResourceConfig(value = {VmInstanceVO.class})
    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "sync clock after vm resume")
    public static GlobalConfig VM_CLOCK_SYNC_AFTER_VM_RESUME = new GlobalConfig(CATEGORY, "vm.clock.sync.after.vm.resume");

    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_UEFI_SECURE_BOOT = new GlobalConfig(CATEGORY, "enable.uefi.secure.boot");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_VM_DEVICE_ADDRESS_RECORDING = new GlobalConfig(CATEGORY, "enable.vm.address.recording");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_VM_INTERNAL_IP_OVERWRITE = new GlobalConfig(CATEGORY, "enable.vm.internal.ip.overwrite");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig UNIQUE_VM_NAME = new GlobalConfig(CATEGORY, "uniqueVmName");

    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "vm.ha.across.clusters")
    public static GlobalConfig VM_HA_ACROSS_CLUSTERS = new GlobalConfig(CATEGORY, "vm.ha.across.clusters");
    @GlobalConfigDef(defaultValue = "AuthenticAMD", type = String.class, description = "set vm cpuid vendor")
    @GlobalConfigValidation(validValues = {"None", "AuthenticAMD"})
    @BindResourceConfig(value = {VmInstanceVO.class})
    public static GlobalConfig VM_CPUID_VENDOR = new GlobalConfig(CATEGORY, "vm.cpuid.vendor");

    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "whether reset TPM state after VM clone")
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig RESET_TPM_AFTER_VM_CLONE = new GlobalConfig(CATEGORY, "reset.tpm.after.vm.clone");

    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "allowed TPM VM start without KMS")
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ALLOWED_TPM_VM_WITHOUT_KMS = new GlobalConfig(CATEGORY, "allowed.tpm.vm.without.kms");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VM_METADATA_ENABLED = new GlobalConfig(CATEGORY, "vm.metadata.enabled");

    @GlobalConfigValidation()
    public static GlobalConfig VM_METADATA_LAST_REFRESH_VERSION = new GlobalConfig(CATEGORY, "vm.metadata.lastRefreshVersion");

    @GlobalConfigValidation(min = 0, max = 100)
    public static GlobalConfig VM_METADATA_FLUSH_CONCURRENCY = new GlobalConfig(CATEGORY, "vm.metadata.flush.concurrency");

    @GlobalConfigValidation(min = 0, max = 300)
    public static GlobalConfig VM_METADATA_FLUSH_POLL_INTERVAL = new GlobalConfig(CATEGORY, "vm.metadata.flush.pollInterval");

    @GlobalConfigValidation(min = 0, max = 1000)
    public static GlobalConfig VM_METADATA_FLUSH_BATCH_SIZE = new GlobalConfig(CATEGORY, "vm.metadata.flush.batchSize");

    @GlobalConfigValidation(min = 0, max = 168)
    public static GlobalConfig VM_METADATA_CLEANUP_GC_INTERVAL = new GlobalConfig(CATEGORY, "vm.metadata.cleanup.gc.interval");

    @GlobalConfigValidation(min = 0, max = 10)
    public static GlobalConfig VM_METADATA_FLUSH_MAX_RETRY = new GlobalConfig(CATEGORY, "vm.metadata.flush.maxRetry");

    @GlobalConfigValidation(min = 0, max = 120)
    public static GlobalConfig VM_METADATA_FLUSH_ZOMBIE_CLAIM_THRESHOLD = new GlobalConfig(CATEGORY, "vm.metadata.flush.zombieClaimThreshold");

    @GlobalConfigValidation(min = 21599, max = 172801)
    public static GlobalConfig VM_METADATA_MAINTENANCE_CONTENT_DRIFT_INTERVAL = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.contentDriftInterval");

    @GlobalConfigValidation(min = 0, max = 86400)
    public static GlobalConfig VM_METADATA_MAINTENANCE_STALE_RECOVERY_INTERVAL = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.staleRecoveryInterval");

    @GlobalConfigValidation(min = 0, max = 1000)
    public static GlobalConfig VM_METADATA_MAINTENANCE_STALE_RECOVERY_MAX_CYCLES = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.staleRecoveryMaxCycles");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_METADATA_PAYLOAD_REJECT_THRESHOLD = new GlobalConfig(CATEGORY, "vm.metadata.payload.rejectThreshold");

    @GlobalConfigValidation(min = 0, max = 86400)
    public static GlobalConfig VM_METADATA_MAINTENANCE_ORPHAN_CHECK_INTERVAL = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.orphanCheckInterval");

    @GlobalConfigValidation(min = 0, max = 20)
    public static GlobalConfig VM_METADATA_MAINTENANCE_STALE_RECOVERY_BATCH_SIZE = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.staleRecoveryBatchSize");

    @GlobalConfigValidation(min = 9, max = 201)
    public static GlobalConfig VM_METADATA_MAINTENANCE_CONTENT_DRIFT_BATCH_SIZE = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.contentDriftBatchSize");

    @GlobalConfigValidation(min = 0, max = 31)
    public static GlobalConfig VM_METADATA_MAINTENANCE_CONTENT_DRIFT_BATCH_SLEEP_SEC = new GlobalConfig(CATEGORY, "vm.metadata.maintenance.contentDriftBatchSleepSec");
}
