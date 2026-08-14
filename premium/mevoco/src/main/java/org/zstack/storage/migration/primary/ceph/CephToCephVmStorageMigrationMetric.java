package org.zstack.storage.migration.primary.ceph;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephSystemTags;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolType;
import org.zstack.storage.ceph.primary.CephPrimaryStoragePoolVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO_;
import org.zstack.storage.migration.VmStorageMigrationMetric;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TagUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by kayo on 2018/11/7.
 */
public class CephToCephVmStorageMigrationMetric implements VmStorageMigrationMetric {
    @Override
    public boolean isCapable(String srcPsType, String dstPsType) {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(srcPsType) &&
                CephConstants.CEPH_PRIMARY_STORAGE_TYPE.equals(dstPsType);
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

    private String getPoolNameFromSystemTags(List<String> systemTags, String volumeType) {
        if (systemTags == null || systemTags.isEmpty()) {
            return null;
        }
        if (VolumeType.Root.toString().equals(volumeType)) {
            return systemTags.stream().filter(tag -> TagUtils.isMatch(CephSystemTags.USE_CEPH_ROOT_POOL.getTagFormat(), tag))
                    .map(tag -> TagUtils.parse(CephSystemTags.USE_CEPH_ROOT_POOL.getTagFormat(), tag).get(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN))
                    .findFirst().orElse(null);
        } else if (VolumeType.Data.toString().equals(volumeType)) {
            return systemTags.stream().filter(tag -> TagUtils.isMatch(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.getTagFormat(), tag))
                    .map(tag -> TagUtils.parse(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL.getTagFormat(), tag).get(CephSystemTags.USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN))
                    .findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public boolean needMigration(VolumeVO vo, String dstPsUuid, String dstHostUuid, List<String> systemTags) {
        if (!vo.getPrimaryStorageUuid().equals(dstPsUuid)) {
            return true;
        }

        String dstPoolName = getPoolNameFromSystemTags(systemTags, vo.getType().toString());
        // no pool specified, skip to migrate
        if (dstPoolName == null) {
            return false;
        }

        String srcPoolName = vo.getInstallPath().replace("ceph://", "").split("/")[0];
        if (dstPoolName.equals(srcPoolName)) {
            return false;
        }
        CephPrimaryStorageVO dstCephPsVO = Q.New(CephPrimaryStorageVO.class).eq(CephPrimaryStorageVO_.uuid, dstPsUuid).find();
        List<String> poolNames;
        if (vo.getType().toString().equals(VolumeType.Root.toString())) {
            poolNames = dstCephPsVO.getPools().stream()
                    .filter(pool -> pool.getType().equals(CephPrimaryStoragePoolType.Root.toString()))
                    .map(CephPrimaryStoragePoolVO::getPoolName).collect(Collectors.toList());
        } else if (vo.getType().toString().equals(VolumeType.Data.toString())) {
            poolNames = dstCephPsVO.getPools().stream()
                    .filter(pool -> pool.getType().equals(CephPrimaryStoragePoolType.Data.toString()))
                    .map(CephPrimaryStoragePoolVO::getPoolName).collect(Collectors.toList());
        } else {
            throw new OperationFailureException(Platform.operr("The type [%s] of volume is invalid.", vo.getType().toString()));
        }

        DebugUtils.Assert(!poolNames.isEmpty(), String.format("cannot find correct pool from ps: %s", dstPsUuid));
        if (poolNames.contains(dstPoolName)) {
            return true;
        }
        throw new OperationFailureException(Platform.operr(String.format("cannot find specified pool[PoolName:%s] from ps: %s", dstPoolName, dstPsUuid)));
    }
}
