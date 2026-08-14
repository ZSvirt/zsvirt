package org.zstack.storage.memorySnapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.migration.StorageMigrationExtensionPoint;
import org.zstack.storage.migration.primary.PrimaryStorageMigrateVmMsg;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;

public class MemoryVolumeMigrationExtensionPoint implements StorageMigrationExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void afterStorageMigration(PrimaryStorageMigrateVmMsg msg, VmInstanceVO vm) {
        new SQLBatch() {

            @Override
            protected void scripts() {
                VolumeVO memoryVolume = q(VolumeVO.class)
                        .eq(VolumeVO_.type, VolumeType.Memory)
                        .eq(VolumeVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                        .find();
                if (memoryVolume == null) {
                    return;
                }

                if (q(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.uuid, memoryVolume.getPrimaryStorageUuid())
                        .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                        .isExists()) {
                    sql(LocalStorageResourceRefVO.class)
                            .eq(LocalStorageResourceRefVO_.resourceUuid, memoryVolume.getUuid())
                            .hardDelete();
                }

                if (q(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.uuid, msg.getDstPrimaryStorageUuid())
                        .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                        .isExists()) {
                    if (q(LocalStorageResourceRefVO.class)
                            .eq(LocalStorageResourceRefVO_.resourceUuid, memoryVolume.getUuid())
                            .eq(LocalStorageResourceRefVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                            .isExists()) {
                        return;
                    }

                    LocalStorageResourceRefVO ref = q(LocalStorageResourceRefVO.class)
                            .eq(LocalStorageResourceRefVO_.resourceUuid, vm.getRootVolume().getUuid())
                            .eq(LocalStorageResourceRefVO_.primaryStorageUuid, msg.getDstPrimaryStorageUuid())
                            .find();

                    LocalStorageResourceRefVO newRef = new LocalStorageResourceRefVO();
                    newRef.setHostUuid(ref.getHostUuid());
                    newRef.setResourceUuid(memoryVolume.getUuid());
                    newRef.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                    newRef.setResourceType(memoryVolume.getResourceType());
                    newRef.setSize(memoryVolume.getSize());
                    persist(newRef);
                }

                memoryVolume.setPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
                merge(memoryVolume);
            }
        }.execute();

    }
}
