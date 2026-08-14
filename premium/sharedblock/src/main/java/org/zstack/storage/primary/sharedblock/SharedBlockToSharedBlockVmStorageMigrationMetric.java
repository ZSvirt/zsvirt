package org.zstack.storage.primary.sharedblock;

import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.VmStorageMigrationMetric;

import java.util.List;

/**
 * Create by weiwang at 2018/6/26
 */
public class SharedBlockToSharedBlockVmStorageMigrationMetric implements VmStorageMigrationMetric {

    @Override
    public boolean isCapable(String srcPsType, String dstPsType) {
        return SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE.equals(srcPsType) &&
                SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE.equals(dstPsType);
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
