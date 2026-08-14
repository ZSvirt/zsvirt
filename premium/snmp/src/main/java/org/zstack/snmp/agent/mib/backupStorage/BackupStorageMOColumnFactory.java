package org.zstack.snmp.agent.mib.backupStorage;

import org.snmp4j.smi.OctetString;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.BackupStorageNamespace;

public class BackupStorageMOColumnFactory extends MOColumnFactory {
    @Override
    public void registerColumns() {
        SnmpGetHandler infoHandler = infoMetricLabelHandler(BackupStorageNamespace.BackupStorageInfo.getName());
        put("uuid", (namespace, metricName, uuid) -> new OctetString(uuid));
        put(BackupStorageNamespace.LabelNames.Name.toString(), infoHandler);
        put(BackupStorageNamespace.AvailableCapacityInBytes.getName());
        put(BackupStorageNamespace.AvailableCapacityInPercent.getName());
        put(BackupStorageNamespace.CPUAllIdleUtilization.getName());
        put(BackupStorageNamespace.CPUAllUsedUtilization.getName());
        put(BackupStorageNamespace.CPUAverageUsedUtilization.getName());
        put(BackupStorageNamespace.CPUAverageUserUtilization.getName());
        put(BackupStorageNamespace.CPUAverageWaitUtilization.getName());
        put(BackupStorageNamespace.CPUAverageSystemUtilization.getName());
        put(BackupStorageNamespace.CPUAverageIdleUtilization.getName());
        put(BackupStorageNamespace.DiskAllReadBytes.getName());
        put(BackupStorageNamespace.DiskAllReadOps.getName());
        put(BackupStorageNamespace.DiskAllWriteBytes.getName());
        put(BackupStorageNamespace.DiskAllWriteOps.getName());
        put(BackupStorageNamespace.MemoryFreeBytes.getName());
        put(BackupStorageNamespace.MemoryFreeInPercent.getName());
        put(BackupStorageNamespace.MemoryUsedBytes.getName());
        put(BackupStorageNamespace.MemoryUsedInPercent.getName());
        put(BackupStorageNamespace.NetworkAllInBytes.getName());
        put(BackupStorageNamespace.NetworkAllInErrors.getName());
        put(BackupStorageNamespace.NetworkAllInPackets.getName());
        put(BackupStorageNamespace.NetworkAllOutBytes.getName());
        put(BackupStorageNamespace.NetworkAllOutErrors.getName());
        put(BackupStorageNamespace.NetworkAllOutPackets.getName());
        put(BackupStorageNamespace.UsedCapacityInBytes.getName());
        put(BackupStorageNamespace.UsedCapacityInPercent.getName());
    }

    @Override
    public String getType() {
        return BackupStorageNamespace.NAME;
    }
}
