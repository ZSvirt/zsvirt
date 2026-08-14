package org.zstack.compute.host;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class HostSystemTags {
    public static String RESERVED_CPU_CAPACITY_TOKEN = "capacity";
    public static PatternedSystemTag RESERVED_CPU_CAPACITY = new PatternedSystemTag(String.format("reservedCpu::{%s}", RESERVED_CPU_CAPACITY_TOKEN), HostVO.class);

    public static String RESERVED_MEMORY_CAPACITY_TOKEN = "capacity";
    public static PatternedSystemTag RESERVED_MEMORY_CAPACITY = new PatternedSystemTag(String.format("reservedMemory::{%s}", RESERVED_MEMORY_CAPACITY_TOKEN), HostVO.class);

    public static SystemTag LIVE_SNAPSHOT = new SystemTag("capability::liveSnapshot", HostVO.class);

    @Deprecated
    public static String OS_DISTRIBUTION_TOKEN = "distribution";
    @Deprecated
    public static PatternedSystemTag OS_DISTRIBUTION = new PatternedSystemTag(String.format("os::distribution::{%s}", OS_DISTRIBUTION_TOKEN), HostVO.class);

    @Deprecated
    public static String OS_RELEASE_TOKEN = "release";
    @Deprecated
    public static PatternedSystemTag OS_RELEASE = new PatternedSystemTag(String.format("os::release::{%s}", OS_RELEASE_TOKEN), HostVO.class);

    @Deprecated
    public static String OS_VERSION_TOKEN = "version";
    @Deprecated
    public static PatternedSystemTag OS_VERSION = new PatternedSystemTag(String.format("os::version::{%s}", OS_VERSION_TOKEN), HostVO.class);

    public static String EXTRA_IPS_TOKEN = "extraips";
    public static PatternedSystemTag EXTRA_IPS = new PatternedSystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), HostVO.class);

    public static final String HOST_CPU_MODEL_NAME_TOKEN = "hostCpuModelName";
    public static PatternedSystemTag HOST_CPU_MODEL_NAME = new PatternedSystemTag(String.format("hostCpuModelName::{%s}", HOST_CPU_MODEL_NAME_TOKEN), HostVO.class);

    public static final String SYSTEM_PRODUCT_NAME_TOKEN = "systemProductName";
    public static PatternedSystemTag SYSTEM_PRODUCT_NAME = new PatternedSystemTag(String.format("systemProductName::{%s}", SYSTEM_PRODUCT_NAME_TOKEN), HostVO.class);

    public static final String SYSTEM_SERIAL_NUMBER_TOKEN = "systemSerialNumber";
    public static PatternedSystemTag SYSTEM_SERIAL_NUMBER = new PatternedSystemTag(String.format("systemSerialNumber::{%s}", SYSTEM_SERIAL_NUMBER_TOKEN), HostVO.class);

    public static final String CPU_GHZ_TOKEN = "cpuGHz";
    public static PatternedSystemTag CPU_GHZ = new PatternedSystemTag(String.format("cpuGHz::{%s}", CPU_GHZ_TOKEN), HostVO.class);

    public static final String CPU_PROCESSOR_NUM_TOKEN = "cpuProcessorNum";
    public static PatternedSystemTag CPU_PROCESSOR_NUM = new PatternedSystemTag(String.format("cpuProcessorNum::{%s}", CPU_PROCESSOR_NUM_TOKEN), HostVO.class);

    public static final String CPU_SOCKETS_TOKEN = "sockets";
    public static final String CPU_CORES_TOKEN = "cores";
    public static final String CPU_THREADS_TOKEN = "threads";
    public static PatternedSystemTag CPU_SOCKET_CORE_THREAD = new PatternedSystemTag(
            String.format("cpuSocketCoreThread::{%s}::{%s}::{%s}", CPU_SOCKETS_TOKEN, CPU_CORES_TOKEN, CPU_THREADS_TOKEN),
            HostVO.class);

    public static final String CPU_CACHE_TOKEN = "cpuCache";
    public static PatternedSystemTag CPU_CACHE = new PatternedSystemTag(String.format("cpuCache::{%s}", CPU_CACHE_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MODEL_NAME_TOKEN = "powerSupplyModelName";
    public static PatternedSystemTag POWER_SUPPLY_MODEL_NAME = new PatternedSystemTag(String.format("powerSupplyModelName::{%s}", POWER_SUPPLY_MODEL_NAME_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MAX_POWER_CAPACITY_TOKEN = "powerSupplyMaxPowerCapacity";
    public static PatternedSystemTag POWER_SUPPLY_MAX_POWER_CAPACITY = new PatternedSystemTag(String.format("powerSupplyMaxPowerCapacity::{%s}", POWER_SUPPLY_MAX_POWER_CAPACITY_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MANUFACTURER_TOKEN = "powerSupplyManufacturer";
    public static PatternedSystemTag POWER_SUPPLY_MANUFACTURER = new PatternedSystemTag(String.format("powerSupplyManufacturer::{%s}", POWER_SUPPLY_MANUFACTURER_TOKEN), HostVO.class);

    public static final String IPMI_ADDRESS_TOKEN = "ipmiAddress";
    public static PatternedSystemTag IPMI_ADDRESS = new PatternedSystemTag(String.format("ipmiAddress::{%s}", IPMI_ADDRESS_TOKEN), HostVO.class);

    public static final String ISCSI_INITIATOR_NAME_TOKEN = "iscsiInitiatorName";
    public static PatternedSystemTag ISCSI_INITIATOR_NAME = new PatternedSystemTag(String.format("iscsiInitiatorName::{%s}", ISCSI_INITIATOR_NAME_TOKEN), HostVO.class);

    public static final String PAGE_TABLE_EXTENSION_DISABLED_TOKEN = "pageTableExtensionDisabled";
    public static PatternedSystemTag PAGE_TABLE_EXTENSION_DISABLED = new PatternedSystemTag(PAGE_TABLE_EXTENSION_DISABLED_TOKEN, HostVO.class);

    public static String HOST_GUEST_TOOLS_VERSION_TOKEN = "guestToolsVersion";
    public static PatternedSystemTag HOST_GUEST_TOOLS =
            new PatternedSystemTag(String.format("GuestTools::{%s}", HOST_GUEST_TOOLS_VERSION_TOKEN), HostVO.class);

    public static String HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN = "guestToolsLinuxVersion";
    public static PatternedSystemTag HOST_LINUX_GUEST_TOOLS =
            new PatternedSystemTag(String.format("GuestToolsLinux::{%s}", HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN), HostVO.class);

    public static String HOST_CONNECTED_TIME_TOKEN = "hostConnectedTime";
    public static PatternedSystemTag HOST_CONNECTED_TIME =
            new PatternedSystemTag(String.format("ConnectedTime::{%s}", HOST_CONNECTED_TIME_TOKEN), HostVO.class);

    public static String SYSTEM_MANUFACTURER_TOKEN = "systemManufacturer";
    public static PatternedSystemTag SYSTEM_MANUFACTURER = new PatternedSystemTag(String.format("systemManufacturer::{%s}", SYSTEM_MANUFACTURER_TOKEN), HostVO.class);

    public static String SYSTEM_UUID_TOKEN = "systemUUID";
    public static PatternedSystemTag SYSTEM_UUID = new PatternedSystemTag(String.format("systemUUID::{%s}", SYSTEM_UUID_TOKEN), HostVO.class);

    public static String BIOS_VENDOR_TOKEN = "biosVendor";
    public static PatternedSystemTag BIOS_VENDOR = new PatternedSystemTag(String.format("biosVendor::{%s}", BIOS_VENDOR_TOKEN), HostVO.class);

    public static String BIOS_VERSION_TOKEN = "biosVersion";
    public static PatternedSystemTag BIOS_VERSION = new PatternedSystemTag(String.format("biosVersion::{%s}", BIOS_VERSION_TOKEN), HostVO.class);

    public static String BIOS_RELEASE_DATE_TOKEN = "biosReleaseDate";
    public static PatternedSystemTag BIOS_RELEASE_DATE = new PatternedSystemTag(String.format("biosReleaseDate::{%s}", BIOS_RELEASE_DATE_TOKEN), HostVO.class);

    public static String BMC_VERSION_TOKEN = "bmcVersion";
    public static PatternedSystemTag BMC_VERSION = new PatternedSystemTag(String.format("bmcVersion::{%s}", BMC_VERSION_TOKEN), HostVO.class);

    public static String UPTIME_TOKEN = "uptime";
    public static PatternedSystemTag UPTIME = new PatternedSystemTag(String.format("uptime::{%s}", UPTIME_TOKEN), HostVO.class);

    public static String MEMORY_SLOTS_MAXIMUM_TOKEN = "memorySlotsMaximum";
    public static PatternedSystemTag MEMORY_SLOTS_MAXIMUM = new PatternedSystemTag(String.format("memorySlotsMaximum::{%s}", MEMORY_SLOTS_MAXIMUM_TOKEN), HostVO.class);
}
