package org.zstack.storage.migration.primary.local;


import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.VmStorageMigrationMetric;
import org.zstack.storage.primary.local.LocalStorageConstants;

import java.util.List;

public class LocalToLocalKvmBlockLiveMigrationMetric implements VmStorageMigrationMetric {
    @Override
    public boolean isCapable(String srcPsType, String dstPsType) {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(srcPsType) &&
                LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(dstPsType);
    }

    @Override
    public boolean isSupportWithDataVolume() {
        return true;
    }

    @Override
    public boolean isSupportWithSnapshot() {
        return false;
    }

    @Override
    public boolean isSupportLive() {
        return true;
    }

    @Override
    public boolean isSupportOffline() {
        return false;
    }

    @Override
    public boolean isSupportSameStorage() {
        return true;
    }

    @Override
    public boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags) {
        if (!vo.getPrimaryStorageUuid().equals(dstPsUuid) || vo.getVmInstanceUuid() == null) {
            return true;
        }

        String srcHostUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vo.getVmInstanceUuid()).select(VmInstanceVO_.hostUuid).findValue();
        return srcHostUuid != null && !srcHostUuid.equals(dstHostUuid);
    }
}
