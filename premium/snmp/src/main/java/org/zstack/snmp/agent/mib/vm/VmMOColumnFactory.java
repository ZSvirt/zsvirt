package org.zstack.snmp.agent.mib.vm;

import org.snmp4j.smi.OctetString;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.VmAbstractNamespace;
import org.zstack.zwatch.namespace.VmNamespace;

public class VmMOColumnFactory extends MOColumnFactory {
    @Override
    public void registerColumns() {
        SnmpGetHandler infoHandler = infoMetricLabelHandler(VmNamespace.VmInfo.getName());
        put("uuid", (namespace, metricName, uuid) -> new OctetString(uuid));
        put(VmAbstractNamespace.LabelNames.Name.toString(), infoHandler);
        put(VmAbstractNamespace.LabelNames.DefaultIpv4.toString(), infoHandler);
        put(VmNamespace.CPUAllIdleUtilization.getName());
        put(VmNamespace.CPUAllUsedUtilization.getName());
        put(VmNamespace.CPUAverageUsedUtilization.getName());
        put(VmNamespace.CPUOccupiedByVM.getName());
        put(VmNamespace.DiskAllFreeCapacityInBytes.getName());
        put(VmNamespace.DiskAllFreeCapacityInPercent.getName());
        put(VmNamespace.DiskAllReadBytes.getName());
        put(VmNamespace.DiskAllReadOps.getName());
        put(VmNamespace.DiskAllUsedCapacityInBytes.getName());
        put(VmNamespace.DiskAllUsedCapacityInPercent.getName());
        put(VmNamespace.DiskAllWriteBytes.getName());
        put(VmNamespace.DiskAllWriteOps.getName());
        put(VmNamespace.MemoryFreeBytes.getName());
        put(VmNamespace.MemoryFreeInPercent.getName());
        put(VmNamespace.MemoryOccupiedByVM.getName());
        put(VmNamespace.MemoryUsedBytes.getName());
        put(VmNamespace.MemoryUsedInPercent.getName());
        put(VmNamespace.NetworkAllInBytes.getName());
        put(VmNamespace.NetworkAllInErrors.getName());
        put(VmNamespace.NetworkAllInPackets.getName());
        put(VmNamespace.NetworkAllOutBytes.getName());
        put(VmNamespace.NetworkAllOutErrors.getName());
        put(VmNamespace.NetworkAllOutPackets.getName());
        put(VmNamespace.OperatingSystemCPUAverageIdleUtilization.getName());
        put(VmNamespace.OperatingSystemCPUAverageSystemUtilization.getName());
        put(VmNamespace.OperatingSystemCPUAverageUsedUtilization.getName());
        put(VmNamespace.OperatingSystemCPUAverageUserUtilization.getName());
        put(VmNamespace.OperatingSystemCPUAverageWaitUtilization.getName());
        put(VmNamespace.OperatingSystemMemoryAvailableBytes.getName());
        put(VmNamespace.OperatingSystemMemoryFreeBytes.getName());
        put(VmNamespace.OperatingSystemMemoryFreePercent.getName());
        put(VmNamespace.OperatingSystemMemoryTotalBytes.getName());
        put(VmNamespace.OperatingSystemMemoryUsedBytes.getName());
        put(VmNamespace.OperatingSystemMemoryUsedPercent.getName());
    }

    @Override
    public String getType() {
        return VmNamespace.NAME;
    }
}
