package org.zstack.compute.cluster;

import org.zstack.core.db.Q;
import org.zstack.header.backup.ManagementNodeRecoverExtensionPoint;
import org.zstack.header.backup.NonBackupInfo;
import org.zstack.header.backup.SystemTagNonBackupInfo;
import org.zstack.header.cluster.ClusterConstant;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.mevoco.MevocoSystemTags;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClusterManagementNodeRecoverExtension implements ManagementNodeRecoverExtensionPoint {

    @Override
    public Map<String, List<NonBackupInfo>> getNonBackupInfos() {
        List<SystemTagInventory> tags = MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTagInventories(
                Q.New(ClusterVO.class).select(ClusterVO_.uuid).listValues());
        return Collections.singletonMap(SystemTagVO.class.getSimpleName(), tags.stream().map(it -> {
            SystemTagNonBackupInfo info = new SystemTagNonBackupInfo();
            info.setTokenName(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN);
            info.setTagFormat(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTagFormat());
            info.setOldValue(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTokenByTag(it.getTag(), MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN));
            info.setUuid(it.getUuid());
            info.setResourceDescription(String.format("migrate cidr of cluster[uuid:%s]", it.getResourceUuid()));
            return info;
        }).collect(Collectors.toList()));
    }

    @Override
    public String getServiceId() {
        return ClusterConstant.SERVICE_ID;
    }
}
