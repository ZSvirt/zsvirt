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
        List<String> clusterUuids = Q.New(ClusterVO.class).select(ClusterVO_.uuid).listValues();
        List<SystemTagInventory> tags = (List) MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTagInventories(clusterUuids);
        List<NonBackupInfo> infos = tags.stream().map(it -> {
            SystemTagNonBackupInfo info = new SystemTagNonBackupInfo();
            info.setTokenName(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN);
            info.setTagFormat(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTagFormat());
            info.setOldValue(MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR.getTokenByTag(it.getTag(), MevocoSystemTags.CLUSTER_MIGRATE_NETWORK_CIDR_TOKEN));
            info.setUuid(it.getUuid());
            info.setResourceDescription(String.format("migrate cidr of cluster[uuid:%s]", it.getResourceUuid()));
            return info;
        }).collect(Collectors.toList());
        infos.addAll((List) MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR.getTagInventories(clusterUuids).stream().map(it -> {
            SystemTagNonBackupInfo info = new SystemTagNonBackupInfo();
            info.setTokenName(MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR_TOKEN);
            info.setTagFormat(MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR.getTagFormat());
            info.setOldValue(MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR.getTokenByTag(it.getTag(), MevocoSystemTags.CLUSTER_CBT_BACKUP_NETWORK_CIDR_TOKEN));
            info.setUuid(it.getUuid());
            info.setResourceDescription(String.format("cbt backup cidr of cluster[uuid:%s]", it.getResourceUuid()));
            return info;
        }).collect(Collectors.toList()));
        return Collections.singletonMap(SystemTagVO.class.getSimpleName(), infos);
    }

    @Override
    public String getServiceId() {
        return ClusterConstant.SERVICE_ID;
    }
}
