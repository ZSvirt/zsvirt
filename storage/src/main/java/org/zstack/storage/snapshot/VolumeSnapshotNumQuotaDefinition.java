package org.zstack.storage.snapshot;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.identity.ResourceHelper;

public class VolumeSnapshotNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VolumeSnapshotQuotaGlobalConfig.VOLUME_SNAPSHOT_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(VolumeSnapshotVO.class, accountUuid);
    }
}
