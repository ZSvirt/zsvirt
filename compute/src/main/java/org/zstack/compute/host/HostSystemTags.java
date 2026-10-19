package org.zstack.compute.host;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class HostSystemTags {
    public static String RESERVED_CPU_CAPACITY_TOKEN = "capacity";
    public static SystemTag RESERVED_CPU_CAPACITY = new SystemTag(String.format("reservedCpu::{%s}", RESERVED_CPU_CAPACITY_TOKEN), HostVO.class);

    public static String RESERVED_MEMORY_CAPACITY_TOKEN = "capacity";
    public static SystemTag RESERVED_MEMORY_CAPACITY = new SystemTag(String.format("reservedMemory::{%s}", RESERVED_MEMORY_CAPACITY_TOKEN), HostVO.class);

    public static SystemTag LIVE_SNAPSHOT = new SystemTag("capability::liveSnapshot", HostVO.class);

    @Deprecated
    public static String OS_DISTRIBUTION_TOKEN = "distribution";
    @Deprecated
    public static SystemTag OS_DISTRIBUTION = new SystemTag(String.format("os::distribution::{%s}", OS_DISTRIBUTION_TOKEN), HostVO.class);

    @Deprecated
    public static String OS_RELEASE_TOKEN = "release";
    @Deprecated
    public static SystemTag OS_RELEASE = new SystemTag(String.format("os::release::{%s}", OS_RELEASE_TOKEN), HostVO.class);

    @Deprecated
    public static String OS_VERSION_TOKEN = "version";
    @Deprecated
    public static SystemTag OS_VERSION = new SystemTag(String.format("os::version::{%s}", OS_VERSION_TOKEN), HostVO.class);

    public static String EXTRA_IPS_TOKEN = "extraips";
    public static SystemTag EXTRA_IPS = new SystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), HostVO.class);

    public static final String HOST_CPU_MODEL_NAME_TOKEN = "hostCpuModelName";
    public static SystemTag HOST_CPU_MODEL_NAME = new SystemTag(String.format("hostCpuModelName::{%s}", HOST_CPU_MODEL_NAME_TOKEN), HostVO.class);

    public static final String SYSTEM_PRODUCT_NAME_TOKEN = "systemProductName";
    public static SystemTag SYSTEM_PRODUCT_NAME = new SystemTag(String.format("systemProductName::{%s}", SYSTEM_PRODUCT_NAME_TOKEN), HostVO.class);

    public static final String SYSTEM_SERIAL_NUMBER_TOKEN = "systemSerialNumber";
    public static SystemTag SYSTEM_SERIAL_NUMBER = new SystemTag(String.format("systemSerialNumber::{%s}", SYSTEM_SERIAL_NUMBER_TOKEN), HostVO.class);

    public static final String CPU_GHZ_TOKEN = "cpuGHz";
    public static SystemTag CPU_GHZ = new SystemTag(String.format("cpuGHz::{%s}", CPU_GHZ_TOKEN), HostVO.class);

    public static final String CPU_PROCESSOR_NUM_TOKEN = "cpuProcessorNum";
    public static SystemTag CPU_PROCESSOR_NUM = new SystemTag(String.format("cpuProcessorNum::{%s}", CPU_PROCESSOR_NUM_TOKEN), HostVO.class);

    public static final String CPU_SOCKETS_TOKEN = "sockets";
    public static final String CPU_CORES_TOKEN = "cores";
    public static final String CPU_THREADS_TOKEN = "threads";
    public static SystemTag CPU_SOCKET_CORE_THREAD = new SystemTag(
            String.format("cpuSocketCoreThread::{%s}::{%s}::{%s}", CPU_SOCKETS_TOKEN, CPU_CORES_TOKEN, CPU_THREADS_TOKEN),
            HostVO.class);

    public static final String CPU_CACHE_TOKEN = "cpuCache";
    public static SystemTag CPU_CACHE = new SystemTag(String.format("cpuCache::{%s}", CPU_CACHE_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MODEL_NAME_TOKEN = "powerSupplyModelName";
    public static SystemTag POWER_SUPPLY_MODEL_NAME = new SystemTag(String.format("powerSupplyModelName::{%s}", POWER_SUPPLY_MODEL_NAME_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MAX_POWER_CAPACITY_TOKEN = "powerSupplyMaxPowerCapacity";
    public static SystemTag POWER_SUPPLY_MAX_POWER_CAPACITY = new SystemTag(String.format("powerSupplyMaxPowerCapacity::{%s}", POWER_SUPPLY_MAX_POWER_CAPACITY_TOKEN), HostVO.class);

    public static final String POWER_SUPPLY_MANUFACTURER_TOKEN = "powerSupplyManufacturer";
    public static SystemTag POWER_SUPPLY_MANUFACTURER = new SystemTag(String.format("powerSupplyManufacturer::{%s}", POWER_SUPPLY_MANUFACTURER_TOKEN), HostVO.class);

    public static final String IPMI_ADDRESS_TOKEN = "ipmiAddress";
    public static SystemTag IPMI_ADDRESS = new SystemTag(String.format("ipmiAddress::{%s}", IPMI_ADDRESS_TOKEN), HostVO.class);

    public static final String ISCSI_INITIATOR_NAME_TOKEN = "iscsiInitiatorName";
    public static SystemTag ISCSI_INITIATOR_NAME = new SystemTag(String.format("iscsiInitiatorName::{%s}", ISCSI_INITIATOR_NAME_TOKEN), HostVO.class);

    public static final String PAGE_TABLE_EXTENSION_DISABLED_TOKEN = "pageTableExtensionDisabled";
    public static SystemTag PAGE_TABLE_EXTENSION_DISABLED = new SystemTag(PAGE_TABLE_EXTENSION_DISABLED_TOKEN, HostVO.class);

    public static String HOST_GUEST_TOOLS_VERSION_TOKEN = "guestToolsVersion";
    public static SystemTag HOST_GUEST_TOOLS =
            new SystemTag(String.format("GuestTools::{%s}", HOST_GUEST_TOOLS_VERSION_TOKEN), HostVO.class);

    public static String HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN = "guestToolsLinuxVersion";
    public static SystemTag HOST_LINUX_GUEST_TOOLS =
            new SystemTag(String.format("GuestToolsLinux::{%s}", HOST_LINUX_GUEST_TOOLS_VERSION_TOKEN), HostVO.class);

    public static String HOST_CONNECTED_TIME_TOKEN = "hostConnectedTime";
    public static SystemTag HOST_CONNECTED_TIME =
            new SystemTag(String.format("ConnectedTime::{%s}", HOST_CONNECTED_TIME_TOKEN), HostVO.class);

    public static String SYSTEM_MANUFACTURER_TOKEN = "systemManufacturer";
    public static SystemTag SYSTEM_MANUFACTURER = new SystemTag(String.format("systemManufacturer::{%s}", SYSTEM_MANUFACTURER_TOKEN), HostVO.class);

    public static String SYSTEM_UUID_TOKEN = "systemUUID";
    public static SystemTag SYSTEM_UUID = new SystemTag(String.format("systemUUID::{%s}", SYSTEM_UUID_TOKEN), HostVO.class);

    public static String BIOS_VENDOR_TOKEN = "biosVendor";
    public static SystemTag BIOS_VENDOR = new SystemTag(String.format("biosVendor::{%s}", BIOS_VENDOR_TOKEN), HostVO.class);

    public static String BIOS_VERSION_TOKEN = "biosVersion";
    public static SystemTag BIOS_VERSION = new SystemTag(String.format("biosVersion::{%s}", BIOS_VERSION_TOKEN), HostVO.class);

    public static String BIOS_RELEASE_DATE_TOKEN = "biosReleaseDate";
    public static SystemTag BIOS_RELEASE_DATE = new SystemTag(String.format("biosReleaseDate::{%s}", BIOS_RELEASE_DATE_TOKEN), HostVO.class);

    public static String BMC_VERSION_TOKEN = "bmcVersion";
    public static SystemTag BMC_VERSION = new SystemTag(String.format("bmcVersion::{%s}", BMC_VERSION_TOKEN), HostVO.class);

    public static String UPTIME_TOKEN = "uptime";
    public static SystemTag UPTIME = new SystemTag(String.format("uptime::{%s}", UPTIME_TOKEN), HostVO.class);

    public static String MEMORY_SLOTS_MAXIMUM_TOKEN = "memorySlotsMaximum";
    public static SystemTag MEMORY_SLOTS_MAXIMUM = new SystemTag(String.format("memorySlotsMaximum::{%s}", MEMORY_SLOTS_MAXIMUM_TOKEN), HostVO.class);
}
