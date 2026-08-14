package org.zstack.storage.ceph;

import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.ChangeVmImageExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.volume.*;
import org.zstack.tag.SystemTagCreator;

import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class CephVmImageChangeExtension implements ChangeVmImageExtensionPoint {
    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.ChangeImage
                || !spec.getVolumeSpecs().get(0).getPrimaryStorageInventory().getType().equals(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
            return;
        }
        //get old ready root volume
        VolumeVO oldRootVolume = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                .eq(VolumeVO_.type, VolumeType.Root)
                .eq(VolumeVO_.status, VolumeStatus.Ready)
                .find();
        //sync root pool tag if exist
        if (oldRootVolume != null && CephSystemTags.USE_CEPH_ROOT_POOL.hasTag(oldRootVolume.getUuid())) {
            String token = CephSystemTags.USE_CEPH_ROOT_POOL.getTokenByResourceUuid(oldRootVolume.getUuid(), CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN);
            SystemTagCreator creator = CephSystemTags.USE_CEPH_ROOT_POOL.newSystemTagCreator(spec.getDestRootVolume().getUuid());
            creator.setTagByTokens(map(e(CephSystemTags.USE_CEPH_ROOT_POOL_TOKEN, token)));
            creator.inherent = false;
            creator.ignoreIfExisting = true;
            creator.create();
        }
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
}
