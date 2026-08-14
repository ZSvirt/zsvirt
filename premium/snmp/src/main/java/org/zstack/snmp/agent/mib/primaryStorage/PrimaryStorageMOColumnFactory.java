package org.zstack.snmp.agent.mib.primaryStorage;

import org.snmp4j.smi.OctetString;
import org.zstack.snmp.agent.mib.MOColumnFactory;
import org.zstack.snmp.agent.mib.SnmpGetHandler;
import org.zstack.zwatch.namespace.CephPrimaryStoragePoolNamespace;
import org.zstack.zwatch.namespace.PrimaryStorageNamespace;

public class PrimaryStorageMOColumnFactory extends MOColumnFactory {
    @Override
    public void registerColumns() {
        SnmpGetHandler infoHandler = infoMetricLabelHandler(PrimaryStorageNamespace.PrimaryStorageInfo.getName());
        put("uuid", (namespace, metricName, uuid) -> new OctetString(uuid));
        put(PrimaryStorageNamespace.LabelNames.Name.toString(), infoHandler);
        put(PrimaryStorageNamespace.AvailableCapacityInBytes.getName());
        put(PrimaryStorageNamespace.AvailableCapacityInPercent.getName());
        put(PrimaryStorageNamespace.AvailablePhysicalCapacityInBytes.getName());
        put(PrimaryStorageNamespace.AvailablePhysicalCapacityInPercent.getName());
        put(PrimaryStorageNamespace.DataVolumeCount.getName());
        put(CephPrimaryStoragePoolNamespace.PoolAvailableCapacityInPercent.getName());
        put(CephPrimaryStoragePoolNamespace.PoolUsedCapacityInPercent.getName());
        put(CephPrimaryStoragePoolNamespace.PoolVirtualAvailableCapacityInPercent.getName());
        put(PrimaryStorageNamespace.RootVolumeCount.getName());
        put(PrimaryStorageNamespace.SnapshotCount.getName());
        put(PrimaryStorageNamespace.TotalPhysicalCapacityInBytes.getName());
        put(PrimaryStorageNamespace.UsedCapacityInBytes.getName());
        put(PrimaryStorageNamespace.UsedCapacityInPercent.getName());
        put(PrimaryStorageNamespace.UsedPhysicalCapacityInBytes.getName());
        put(PrimaryStorageNamespace.UsedPhysicalCapacityInPercent.getName());
    }

    @Override
    public String getType() {
        return PrimaryStorageNamespace.NAME;
    }
}
