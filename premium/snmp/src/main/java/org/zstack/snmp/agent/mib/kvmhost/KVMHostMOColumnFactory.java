package org.zstack.snmp.agent.mib.kvmhost;

import org.snmp4j.smi.OctetString;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.HostAbstractNamespace;
import org.zstack.zwatch.namespace.KVMHostNamespace;

/**
 *
 * @Author : jingwang
 * @create 2023/7/28 11:57 AM
 */

public class KVMHostMOColumnFactory extends MOColumnFactory {
    @Override
    public void registerColumns() {
        SnmpGetHandler infoHandler = infoMetricLabelHandler(HostAbstractNamespace.HostInfo.getName());
        put("uuid", (namespace, metricName, uuid) -> new OctetString(uuid));
        put(HostAbstractNamespace.LabelNames.Name.toString(), infoHandler);
        put(HostAbstractNamespace.LabelNames.ManagementIp.toString(), infoHandler);
        put(KVMHostNamespace.CPUAllIdleUtilization.getName());
        put(KVMHostNamespace.CPUAllUsedUtilization.getName());
        put(KVMHostNamespace.CPUAverageIdleUtilization.getName());
        put(KVMHostNamespace.CPUAverageSystemUtilization.getName());
        put(KVMHostNamespace.CPUAverageUsedUtilization.getName());
        put(KVMHostNamespace.CPUAverageUserUtilization.getName());
        put(KVMHostNamespace.CPUAverageWaitUtilization.getName());
        put(KVMHostNamespace.DiskAllFreeCapacityInBytes.getName());
        put(KVMHostNamespace.DiskAllFreeCapacityInPercent.getName());
        put(KVMHostNamespace.DiskAllReadBytes.getName());
        put(KVMHostNamespace.DiskAllReadOps.getName());
        put(KVMHostNamespace.DiskAllUsedCapacityInBytes.getName());
        put(KVMHostNamespace.DiskAllUsedCapacityInPercent.getName());
        put(KVMHostNamespace.DiskAllWriteBytes.getName());
        put(KVMHostNamespace.DiskAllWriteOps.getName());
        put(KVMHostNamespace.DiskRootUsedCapacityInBytes.getName());
        put(KVMHostNamespace.DiskRootUsedCapacityInPercent.getName());
        put(KVMHostNamespace.DiskTotalCapacityInBytes.getName());
        put(KVMHostNamespace.DiskTransUsedCapacityInBytes.getName());
        put(KVMHostNamespace.DiskTransUsedCapacityInPercent.getName());
        put(KVMHostNamespace.DiskZStackUsedCapacityInBytes.getName());
        put(KVMHostNamespace.DiskZStackUsedCapacityInPercent.getName());
        put(KVMHostNamespace.IpmiStatus.getName());
        put(KVMHostNamespace.MemoryFreeBytes.getName());
        put(KVMHostNamespace.MemoryFreeInPercent.getName());
        put(KVMHostNamespace.MemoryUsedBytes.getName());
        put(KVMHostNamespace.MemoryUsedInPercent.getName());
        put(KVMHostNamespace.NetworkAllInBytes.getName());
        put(KVMHostNamespace.NetworkAllInErrors.getName());
        put(KVMHostNamespace.NetworkAllInPackets.getName());
        put(KVMHostNamespace.NetworkAllOutBytes.getName());
        put(KVMHostNamespace.NetworkAllOutErrors.getName());
        put(KVMHostNamespace.NetworkAllOutPackets.getName());
        put(KVMHostNamespace.NetworkConntrackCount.getName());
        put(KVMHostNamespace.NetworkConntrackInPercent.getName());
    }

    @Override
    public String getType() {
        return KVMHostNamespace.NAME;
    }
}
