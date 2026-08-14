package org.zstack.storage.primary.sharedblock;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.volume.*;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;

import java.util.Arrays;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class SharedBlockVolumeFactory implements CreateDataVolumeExtensionPoint {
    @Autowired
    protected TagManager tagMgr;

    @Override
    public void preCreateVolume(VolumeCreateMessage msg) {

    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {

    }

    private boolean isSharedBlockPs(String psUuid) {
        return Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)
                .isExists();
    }

    @Override
    public void afterCreateVolume(VolumeVO volume) {
        if (!VolumeType.Memory.equals(volume.getType())) {
            return;
        }
        if (!isSharedBlockPs(volume.getPrimaryStorageUuid())) {
            return;
        }
        tagMgr.createNonInherentSystemTags(Arrays.asList(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY
                        .instantiateTag(map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, VolumeProvisioningStrategy.ThickProvisioning.toString()))))
                , volume.getUuid(), VolumeVO.class.getSimpleName());
    }
}
