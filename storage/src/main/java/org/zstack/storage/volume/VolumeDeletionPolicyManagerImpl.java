package org.zstack.storage.volume;

import org.zstack.header.volume.VolumeDeletionPolicyManager;

/**
 * Created by frank on 11/12/2015.
 */
public class VolumeDeletionPolicyManagerImpl implements VolumeDeletionPolicyManager {
    @Override
    public VolumeDeletionPolicy getDeletionPolicy(String volumeUuid) {
        return VolumeDeletionPolicy.valueOf(VolumeGlobalConfig.VOLUME_DELETION_POLICY.value(String.class));
    }
}
