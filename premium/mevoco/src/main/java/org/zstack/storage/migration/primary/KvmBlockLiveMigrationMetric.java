package org.zstack.storage.migration.primary;

import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.VmStorageMigrationMetric;

import java.util.List;

public class KvmBlockLiveMigrationMetric implements VmStorageMigrationMetric {
    @Override
    public boolean isCapable(String srcPsType, String dstPsType) {
        return true;
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
        return false;
    }

    @Override
    public boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags) {
        return !vo.getPrimaryStorageUuid().equals(dstPsUuid);
    }
}
