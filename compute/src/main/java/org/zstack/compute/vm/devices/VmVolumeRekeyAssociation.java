package org.zstack.compute.vm.devices;

import org.zstack.core.db.Q;
import org.zstack.header.keyprovider.KeyProviderRekeyAssociationExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class VmVolumeRekeyAssociation implements KeyProviderRekeyAssociationExtensionPoint {
    @Override
    public String getType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public String getAssociatedResourceType() {
        return VolumeVO.class.getSimpleName();
    }

    @Override
    public List<String> getAssociatedResourceUuids(List<String> resourceUuids) {
        if (CollectionUtils.isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }

        return Q.New(VolumeVO.class)
                .in(VolumeVO_.vmInstanceUuid, resourceUuids)
                .notEq(VolumeVO_.type, VolumeType.Memory)
                .select(VolumeVO_.uuid)
                .listValues();
    }
}
