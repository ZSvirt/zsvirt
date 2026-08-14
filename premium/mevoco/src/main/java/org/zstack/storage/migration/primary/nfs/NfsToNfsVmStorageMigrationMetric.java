package org.zstack.storage.migration.primary.nfs;

import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.VmStorageMigrationMetric;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;

import java.util.List;

/**
 * Created by kayo on 2018/11/7.
 */
public class NfsToNfsVmStorageMigrationMetric implements VmStorageMigrationMetric {
    @Override
    public boolean isCapable(String srcPsType, String dstPsType) {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE.equals(srcPsType) &&
                NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE.equals(dstPsType);
    }

    @Override
    public boolean isSupportWithDataVolume() {
        return true;
    }

    @Override
    public boolean isSupportWithSnapshot() {
        return true;
    }

    @Override
    public boolean isSupportLive() {
        return false;
    }

    @Override
    public boolean isSupportOffline() {
        return true;
    }

    @Override
    public boolean isSupportSameStorage() {
        return false;
    }

    @Override
    public boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags) {
        return !vo.getPrimaryStorageUuid().equals(dstPsUuid);
    }
}
