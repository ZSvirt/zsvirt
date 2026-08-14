package org.zstack.storage.migration;

import org.zstack.header.volume.VolumeVO;

import java.util.List;

/**
 * Create by weiwang at 2018/6/26
 */
public interface VmStorageMigrationMetric {
    boolean isCapable(String srcPsType, String dstPsType);

    boolean isSupportWithDataVolume();

    boolean isSupportWithSnapshot();

    boolean isSupportLive();

    boolean isSupportOffline();

    boolean isSupportSameStorage();

    boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags);
}
