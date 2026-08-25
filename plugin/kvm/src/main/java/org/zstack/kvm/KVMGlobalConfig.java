package org.zstack.kvm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.zone.ZoneVO;

import static org.zstack.kvm.KVMConstant.EDK_VERSION_NONE;

/**
 */
@GlobalConfigDefinition
public class KVMGlobalConfig {
    public static final String CATEGORY = "kvm";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_MIGRATION_QUANTITY = new GlobalConfig(CATEGORY, "vm.migrationQuantity");
    @GlobalConfigValidation(min = -1)
    public static GlobalConfig RESERVED_CPU_CAPACITY = new GlobalConfig(CATEGORY, "reservedCpu");
    @GlobalConfigValidation
    @BindResourceConfig({HostVO.class, ClusterVO.class, ZoneVO.class})
    public static GlobalConfig RESERVED_MEMORY_CAPACITY = new GlobalConfig(CATEGORY, "reservedMemory");
    @GlobalConfigValidation(inNumberRange = {0, 24})
    public static GlobalConfig MAX_DATA_VOLUME_NUM = new GlobalConfig(CATEGORY, "dataVolume.maxNum");
    @GlobalConfigValidation(min = 2)
    public static GlobalConfig HOST_SYNC_LEVEL = new GlobalConfig(CATEGORY, "host.syncLevel");
    @GlobalConfigValidation(min = 2)
    public static GlobalConfig HOST_SNAPSHOT_SYNC_LEVEL = new GlobalConfig(CATEGORY, "host.snapshot.syncLevel");
    @GlobalConfigValidation(inNumberRange = {1, 10})
    public static GlobalConfig VM_CREATE_CONCURRENCY = new GlobalConfig(CATEGORY, "vm.createConcurrency");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_ALIYUN = new GlobalConfig(CATEGORY, "host.DNSCheckAliyun");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_163 = new GlobalConfig(CATEGORY, "host.DNSCheck163");
    @GlobalConfigValidation
    public static GlobalConfig HOST_DNS_CHECK_LIST = new GlobalConfig(CATEGORY, "host.DNSCheckList");
    @GlobalConfigValidation
    public static GlobalConfig ALLOW_LIVE_SNAPSHOT_ON_REDHAT = new GlobalConfig(CATEGORY, "redhat.liveSnapshotOn");
    @GlobalConfigValidation(validValues = {"none", "writethrough", "writeback"})
    @BindResourceConfig({VolumeVO.class, VmInstanceVO.class})
    public static GlobalConfig LIBVIRT_CACHE_MODE = new GlobalConfig(CATEGORY, "vm.cacheMode");
    @GlobalConfigValidation(validValues = {"none", "host-model", "host-passthrough",
            "Dhyana", "EPYC", "EPYC-IBPB", "Haswell", "Haswell-noTSX", "Broadwell", "Broadwell-noTSX",
            "SandyBridge", "IvyBridge", "Conroe", "Penryn", "Nehalem", "Westmere", "Opteron_G1",
            "Opteron_G2", "Opteron_G3", "Opteron_G4", "pentium", "pentium2", "pentium3",
            "Kunpeng-920", "FT-2000+", "Tengyun-S2500"})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig NESTED_VIRTUALIZATION = new GlobalConfig(CATEGORY, "vm.cpuMode");
    @GlobalConfigValidation
    public static GlobalConfig VM_SYNC_ON_HOST_PING = new GlobalConfig(CATEGORY, "vmSyncOnHostPing");
    @GlobalConfigValidation
    public static GlobalConfig CHECK_HOST_CPU_MODEL_NAME = new GlobalConfig(CATEGORY, "checkHostCpuModelName");
    @GlobalConfigValidation
    @BindResourceConfig({ClusterVO.class})
    public static GlobalConfig KVM_IGNORE_MSRS = new GlobalConfig(CATEGORY, "ignoreMsrs");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({ClusterVO.class})
    public static GlobalConfig AUTO_VM_NIC_MULTIQUEUE = new GlobalConfig(CATEGORY, "auto.set.vm.nic.multiqueue");
    @GlobalConfigValidation
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig MIGRATE_AUTO_CONVERGE = new GlobalConfig(CATEGORY, "migrate.autoConverge");
    @GlobalConfigValidation
    public static GlobalConfig MIGRATE_XBZRLE = new GlobalConfig(CATEGORY, "migrate.xbzrle");
    @GlobalConfigValidation(min = 0, max = 3600)
    public static GlobalConfig TEST_SSH_PORT_ON_OPEN_TIMEOUT = new GlobalConfig(CATEGORY, "testSshPortOpenTimeout");
    @GlobalConfigValidation(min = 0, max = 300)
    public static GlobalConfig TEST_SSH_PORT_ON_CONNECT_TIMEOUT = new GlobalConfig(CATEGORY, "testSshPortOnConnectTimeout");
    @GlobalConfigValidation
    public static GlobalConfig RESTART_AGENT_IF_FAKE_DEAD = new GlobalConfig(CATEGORY, "restartagentwhenfakedead");

    @GlobalConfigValidation
    public static GlobalConfig KVMAGENT_ALLOW_PORTS_LIST = new GlobalConfig(CATEGORY, "kvmagent.allow.ports");

    @GlobalConfigValidation
    public static GlobalConfig ENABLE_HOST_TCP_CONNECTION_CHECK = new GlobalConfig(CATEGORY, "enable.host.tcp.connection.check");
    @GlobalConfigValidation
    public static GlobalConfig HOST_CONNECTION_CHECK_INTERVAL = new GlobalConfig(CATEGORY, "host.connection.check.interval");
    @GlobalConfigValidation
    public static GlobalConfig CONNECTION_SERVER_UPDATE_INTERVAL = new GlobalConfig(CATEGORY, "connection.server.update.interval");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ENABLE_VM_MIGRATION_HOST_CPU_FUNCTION_CHECK = new GlobalConfig(CATEGORY, "enable.vm.migration.host.cpu.function.check");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig REDIRECT_CONSOLE_LOG_TO_FILE = new GlobalConfig(CATEGORY, "redirect.vm.log.to.file");

    // vm cpu features stored in cpuid
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_CPU_HYPERVISOR_FEATURE = new GlobalConfig(CATEGORY, "vm.cpu.hypervisor.feature");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig VM_HYPERV_CLOCK_FEATURE = new GlobalConfig(CATEGORY, "vm.hyperv.clock.feature");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig SUSPEND_TO_RAM = new GlobalConfig(CATEGORY, "vm.suspend.to.ram");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig SUSPEND_TO_DISK = new GlobalConfig(CATEGORY, "vm.suspend.to.disk");

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig WEBSSH_IDLE_TIMEOUT = new GlobalConfig(CATEGORY, "webssh.idleTimeout");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "8888", type = Long.class, description = "the default port used by web-based SSH server")
    public static GlobalConfig HOST_WEBSSH_PORT = new GlobalConfig(CATEGORY, "host.webssh.port");
    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "8889", type = Long.class, description = "the default https port used by web-based SSH server")
    public static GlobalConfig HOST_WEBSSH_HTTPS_PORT = new GlobalConfig(CATEGORY, "host.webssh.https.port");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable install host shutdown hook")
    public static GlobalConfig INSTALL_HOST_SHUTDOWN_HOOK = new GlobalConfig(CATEGORY, "install.host.shutdown.hook");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", type = Boolean.class, description = "enable memory auto balloon")
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig MEMORY_AUTO_BALLOON = new GlobalConfig(CATEGORY, "memory.auto.balloon");

    @GlobalConfigValidation(validValues = {"true", "false", "none"})
    @GlobalConfigDef(defaultValue = "none", description = "enable host ksm")
    @BindResourceConfig({HostVO.class})
    public static GlobalConfig HOST_KSM = new GlobalConfig(CATEGORY, "host.ksm");

    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "use 'cold' mode when stop VM without operating system")
    public static GlobalConfig STOP_VM_WITHOUT_OS_BY_FORCE_MODE =
            new GlobalConfig(CATEGORY, "stop.vm.without.os.by.force.mode");

    @GlobalConfigValidation(min = 10000, max = 1000000)
    @GlobalConfigDef(defaultValue = "1000000",type = Long.class)
    @BindResourceConfig(VmInstanceVO.class)
    public static GlobalConfig VM_CPU_QUOTA = new GlobalConfig(CATEGORY,"vm.cpu.quota");

    @GlobalConfigValidation(validValues = {"true", "false"})
    @GlobalConfigDef(defaultValue = "false", description = "restart kvm host libvirtd service or not")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE = new GlobalConfig(CATEGORY, "reconnect.host.restart.libvirtd.service");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(
            defaultValue = "2147483648",
            description = "The threshold for the physical memory usage of the kvmagent process, exceeding which an alarm will be triggered.",
            type = Long.class
    )
    public static GlobalConfig KVMAGENT_PHYSICAL_MEMORY_USAGE_ALARM_THRESHOLD = new GlobalConfig(CATEGORY, "kvmagent.physicalmemory.usage.alarm.threshold");

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(
            defaultValue = "10737418240",
            description = "The hard limit for the physical memory usage of the kvmagent process, exceeding this value will trigger a kvmagent restart.",
            type = Long.class
    )
    public static GlobalConfig KVMAGENT_PHYSICAL_MEMORY_USAGE_HARD_LIMIT = new GlobalConfig(CATEGORY, "kvmagent.physicalmemory.usage.hardlimit");

    @GlobalConfigDef(defaultValue = EDK_VERSION_NONE,
            description = "Specify the EDK version to be used for the next VM startup. None indicates the use of the system's default EDK version")
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_EDK_VERSION_CONFIG = new GlobalConfig(CATEGORY, "vm.edk.version");

    @GlobalConfigValidation(min = 1, max = 86400)
    @GlobalConfigDef(defaultValue = "15", type = Long.class,
            description = "Interval in seconds for checking VM host files (NvRam, TpmState) on KVM hosts")
    public static GlobalConfig VM_HOST_FILE_SYNC_INTERVAL = new GlobalConfig(CATEGORY, "vm.host.file.sync.interval");

    @GlobalConfigValidation(min = 1, max = 30)
    @GlobalConfigDef(defaultValue = "5", type = Integer.class,
            description = "The concurrency level for syncing VM host files from KVM hosts")
    public static GlobalConfig VM_HOST_FILE_SYNC_CONCURRENCY = new GlobalConfig(CATEGORY, "vm.host.file.sync.concurrency");
}
