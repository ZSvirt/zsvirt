package org.zstack.snmp.agent.mib.vrouter;

import org.snmp4j.smi.OctetString;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.VRouterNamespace;
import org.zstack.zwatch.namespace.VmAbstractNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

public class VRouterMOColumnFactory extends MOColumnFactory {
    @Override
    public void registerColumns() {
        SnmpGetHandler infoHandler = infoMetricLabelHandler(VmNamespace.VmInfo.getName());
        put("uuid", (namespace, metricName, uuid) -> new OctetString(uuid));
        put(VmAbstractNamespace.LabelNames.Name.toString(), infoHandler);
        put(VmAbstractNamespace.LabelNames.DefaultIpv4.toString(), infoHandler);
        put(VRouterNamespace.CPUAllIdleUtilization.getName());
        put(VRouterNamespace.CPUAllUsedUtilization.getName());
        put(VRouterNamespace.CPUAverageUsedUtilization.getName());
        put(VRouterNamespace.DiskAllFreeCapacityInBytes.getName());
        put(VRouterNamespace.DiskAllFreeCapacityInPercent.getName());
        put(VRouterNamespace.DiskAllReadBytes.getName());
        put(VRouterNamespace.DiskAllReadOps.getName());
        put(VRouterNamespace.DiskAllUsedCapacityInBytes.getName());
        put(VRouterNamespace.DiskAllUsedCapacityInPercent.getName());
        put(VRouterNamespace.DiskAllWriteBytes.getName());
        put(VRouterNamespace.DiskAllWriteOps.getName());
        put(VRouterNamespace.MemoryFreeBytes.getName());
        put(VRouterNamespace.MemoryFreeInPercent.getName());
        put(VRouterNamespace.MemoryOccupiedByVM.getName());
        put(VRouterNamespace.MemoryUsedBytes.getName());
        put(VRouterNamespace.MemoryUsedInPercent.getName());
        put(VRouterNamespace.NetworkAllInBytes.getName());
        put(VRouterNamespace.NetworkAllInErrors.getName());
        put(VRouterNamespace.NetworkAllInPackets.getName());
        put(VRouterNamespace.NetworkAllOutBytes.getName());
        put(VRouterNamespace.NetworkAllOutErrors.getName());
        put(VRouterNamespace.NetworkAllOutPackets.getName());
        put(VRouterNamespace.OperatingSystemCPUAverageIdleUtilization.getName());
        put(VRouterNamespace.OperatingSystemCPUAverageSystemUtilization.getName());
        put(VRouterNamespace.OperatingSystemCPUAverageUsedUtilization.getName());
        put(VRouterNamespace.OperatingSystemCPUAverageUserUtilization.getName());
        put(VRouterNamespace.OperatingSystemCPUAverageWaitUtilization.getName());
        put(VRouterNamespace.OperatingSystemMemoryAvailableBytes.getName());
        put(VRouterNamespace.OperatingSystemMemoryFreeBytes.getName());
        put(VRouterNamespace.OperatingSystemMemoryFreePercent.getName());
        put(VRouterNamespace.OperatingSystemMemoryTotalBytes.getName());
        put(VRouterNamespace.OperatingSystemMemoryUsedBytes.getName());
        put(VRouterNamespace.OperatingSystemMemoryUsedPercent.getName());
        put(VRouterNamespace.PVPanicEnableInDomainXML.getName());
        put(VRouterNamespace.VRouterCPUAverageIdleUtilization.getName());
        put(VRouterNamespace.VRouterCPUAverageSystemUtilization.getName());
        put(VRouterNamespace.VRouterCPUAverageUsedUtilization.getName());
        put(VRouterNamespace.VRouterCPUAverageUserUtilization.getName());
        put(VRouterNamespace.VRouterCPUAverageWaitUtilization.getName());
        put(VRouterNamespace.VRouterDiskAllFreeCapacityInBytes.getName());
        put(VRouterNamespace.VRouterDiskAllFreeCapacityInPercent.getName());
        put(VRouterNamespace.VRouterDiskAllUsedCapacityInBytes.getName());
        put(VRouterNamespace.VRouterDiskAllUsedCapacityInPercent.getName());
        put(VRouterNamespace.VRouterMemoryAvailableBytes.getName());
        put(VRouterNamespace.VRouterMemoryFreeBytes.getName());
        put(VRouterNamespace.VRouterMemoryFreePercent.getName());
        put(VRouterNamespace.VRouterMemoryTotalBytes.getName());
        put(VRouterNamespace.VRouterMemoryUsedBytes.getName());
        put(VRouterNamespace.VRouterMemoryUsedPercent.getName());
        put(VRouterNamespace.ZWatchAgentFeaturePvpanic.getName());
    }

    @Override
    public String getType() {
        return VRouterNamespace.NAME;
    }
}
