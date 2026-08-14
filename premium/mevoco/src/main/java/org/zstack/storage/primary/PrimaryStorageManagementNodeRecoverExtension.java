package org.zstack.storage.primary;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.backup.ManagementNodeRecoverExtensionPoint;
import org.zstack.header.backup.NonBackupInfo;
import org.zstack.header.backup.SystemTagNonBackupInfo;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrimaryStorageManagementNodeRecoverExtension implements ManagementNodeRecoverExtensionPoint {
    @Override
    public Map<String, List<NonBackupInfo>> getNonBackupInfos() {
        Map<String, PrimaryStorageVO> ps = new HashMap<>();
        Q.New(PrimaryStorageVO.class).list().forEach(it -> ps.put(((PrimaryStorageVO)it).getUuid(), (PrimaryStorageVO)it));

        List<SystemTagInventory> tags = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTagInventories(
                Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.uuid).listValues());
        return Collections.singletonMap(SystemTagVO.class.getSimpleName(), tags.stream().map(it -> {
            SystemTagNonBackupInfo info = new SystemTagNonBackupInfo();
            info.setTokenName(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
            info.setTagFormat(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTagFormat());
            info.setOldValue(PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByTag(it.getTag(), PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN));
            info.setUuid(it.getUuid());
            List<String> attachedClusterUuids = ps.get(it.getResourceUuid()).getAttachedClusterRefs().stream().map(PrimaryStorageClusterRefVO::getClusterUuid).collect(Collectors.toList());
            info.setResourceDescription(String.format("storage cidr of primary storage[uuid:%s, attached cluster[uuids:%s]]",
                    it.getResourceUuid(), attachedClusterUuids));
            return info;
        }).collect(Collectors.toList()));
    }

    @Override
    public String getServiceId() {
        return PrimaryStorageConstant.SERVICE_ID;
    }
}
