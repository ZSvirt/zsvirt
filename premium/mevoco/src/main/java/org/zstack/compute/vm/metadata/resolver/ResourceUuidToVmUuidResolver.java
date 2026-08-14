package org.zstack.compute.vm.metadata.resolver;

import org.zstack.core.db.Q;
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;

/**
 * Resolves a resource UUID (VM / Volume / Nic) to a VM UUID.
 *
 * <p>Used by tag and config APIs whose {@code field} points to a resourceUuid
 * that is directly one of VmInstanceVO, VolumeVO, or VmNicVO.</p>
 */
public class ResourceUuidToVmUuidResolver implements VmUuidFromApiResolver {

    @Override
    public String resolveVmUuid(String fieldValue) {
        if (fieldValue == null) {
            return null;
        }

        // Fast path: the resource UUID is itself a VM UUID (most common for tag/config APIs)
        boolean isVm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, fieldValue).isExists();
        if (isVm) {
            return fieldValue;
        }

        // Volume: single query fetching both vmInstanceUuid and lastVmInstanceUuid
        Tuple volTuple = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, fieldValue)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.lastVmInstanceUuid).findTuple();
        if (volTuple != null) {
            String vmUuid = volTuple.get(0, String.class);
            return vmUuid != null ? vmUuid : volTuple.get(1, String.class);
        }

        return Q.New(VmNicVO.class).eq(VmNicVO_.uuid, fieldValue).select(VmNicVO_.vmInstanceUuid).findValue();
    }
}
