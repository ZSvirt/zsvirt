package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;

/**
 * volumeUuid → VolumeVO.vmInstanceUuid (fallback lastVmInstanceUuid).
 */
public class VolumeUuidToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        // Single query fetching both vmInstanceUuid and lastVmInstanceUuid
        Tuple volTuple = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, fieldValue)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.lastVmInstanceUuid).findTuple();
        if (volTuple != null) {
            String vmUuid = volTuple.get(0, String.class);
            return vmUuid != null ? vmUuid : volTuple.get(1, String.class);
        }

        return null;
    }
}
