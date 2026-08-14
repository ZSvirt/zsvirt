package org.zstack.imagereplicator;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO_;

import static org.zstack.core.Platform.operr;

@InterceptorForService("imagereplicator")
public class ReplicationGroupApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddBackupStoragesToReplicationGroupMsg) {
            validate((APIAddBackupStoragesToReplicationGroupMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddBackupStoragesToReplicationGroupMsg msg) {
        if (Q.New(ImageReplicationGroupBackupStorageRefVO.class)
                .in(ImageReplicationGroupBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuids())
                .eq(ImageReplicationGroupBackupStorageRefVO_.replicationGroupUuid, msg.getReplicationGroupUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(
                    operr("One or more backup storage[uuids:%s] has been added to replication group[uuid:%s]",
                            String.join(",", msg.getBackupStorageUuids()),
                            msg.getReplicationGroupUuid())
            );
        }

        msg.getBackupStorageUuids().forEach(bsUuid -> {
            if (!Q.New(ImageStoreBackupStorageVO.class)
                    .eq(ImageStoreBackupStorageVO_.uuid, bsUuid)
                    .isExists()) {
                throw new ApiMessageInterceptionException(
                        operr("Backup storage[uuids:%s] is not of type ImageStore", bsUuid)
                );
            }
        });

        msg.getBackupStorageUuids().forEach(bsUuid -> {
            if (!Q.New(BackupStorageZoneRefVO.class)
                    .eq(BackupStorageZoneRefVO_.backupStorageUuid, bsUuid)
                    .isExists()) {
                throw new ApiMessageInterceptionException(
                        operr("Backup storage[uuids:%s] is not attached to any Zone", bsUuid)
                );
            }
        });
    }
}
