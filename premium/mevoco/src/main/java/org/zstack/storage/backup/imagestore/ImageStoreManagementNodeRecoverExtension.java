package org.zstack.storage.backup.imagestore;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.backup.ManagementNodeRecoverExtensionPoint;
import org.zstack.header.backup.NonBackupInfo;
import org.zstack.header.storage.backup.BackupStorageConstant;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageStoreManagementNodeRecoverExtension implements ManagementNodeRecoverExtensionPoint {
    @Override
    public Map<String, List<NonBackupInfo>> getNonBackupInfos() {
        List<ImageStoreBackupStorageVO> bs = Q.New(ImageStoreBackupStorageVO.class).list();
        return Collections.singletonMap(ImageStoreBackupStorageVO.class.getSimpleName(), bs.stream().map(it -> {
            NonBackupInfo info = new NonBackupInfo();
            info.setName(it.getName());
            info.setAttributeName("hostname");
            info.setOldValue(it.getHostname());
            info.setUuid(it.getUuid());
            return info;
        }).collect(Collectors.toList()));
    }

    @Override
    public String getServiceId() {
        return BackupStorageConstant.SERVICE_ID;
    }
}
