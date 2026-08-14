package org.zstack.storage.migration;

import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  *  *  *  *  * Created by LiangHanYu on 2020/8/28 17:23
 *   *   *   *   *   */
public final class LiveMigrationFunction {

    public static long getVolumeSizeToMigrate(VmInstanceVO vmInstanceVO, String DstPrimaryStorageUuid) {
        final List<VolumeVO> finalVolumes = getVolumesToMigrate(vmInstanceVO,DstPrimaryStorageUuid);
        long size = finalVolumes.stream()
                .map(VolumeAO::getSize)
                .reduce(0L, Long::sum);
        return size;
    }

    public static List<VolumeVO> getVolumesToMigrate(VmInstanceVO vmInstanceVO, String DstPrimaryStorageUuid) {
        List<VolumeVO> volumes = new ArrayList<>();
        volumes.addAll(vmInstanceVO.getAllVolumes());

        return psIsLocal(DstPrimaryStorageUuid) ?
                volumes :
                volumes.stream()
                        .filter(v -> !v.getPrimaryStorageUuid().equals(DstPrimaryStorageUuid))
                        .collect(Collectors.toList());
    }

    static boolean psIsLocal(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .isExists();
    }
}
