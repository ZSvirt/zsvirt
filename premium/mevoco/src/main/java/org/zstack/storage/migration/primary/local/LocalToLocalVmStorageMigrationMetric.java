package org.zstack.storage.migration.primary.local;


import org.zstack.core.db.Q;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.VmStorageMigrationMetric;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.local.LocalStorageSystemTags;
import org.zstack.tag.PatternedSystemTag;

import java.util.List;

public class LocalToLocalVmStorageMigrationMetric implements VmStorageMigrationMetric {
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
        return true;
    }

    @Override
    public boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags) {
        if (dstHostUuid == null && !systemTags.isEmpty()) {
            PatternedSystemTag lsTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME;
            dstHostUuid = systemTags.stream().filter(lsTag::isMatch)
                    .map(it -> lsTag.getTokenByTag(it, LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN))
                    .findAny().orElse(null);
        }

        String srcHostUuid = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, vo.getUuid())
                .select(LocalStorageResourceRefVO_.hostUuid).findValue();

        return srcHostUuid != null && !srcHostUuid.equals(dstHostUuid);
    }
}
