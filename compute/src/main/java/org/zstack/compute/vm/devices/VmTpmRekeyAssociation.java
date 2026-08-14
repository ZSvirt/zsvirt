package org.zstack.compute.vm.devices;

import org.zstack.core.db.Q;
import org.zstack.header.keyprovider.KeyProviderRekeyAssociationExtensionPoint;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class VmTpmRekeyAssociation implements KeyProviderRekeyAssociationExtensionPoint {
    @Override
    public String getType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public String getAssociatedResourceType() {
        return TpmVO.class.getSimpleName();
    }

    @Override
    public List<String> getAssociatedResourceUuids(List<String> resourceUuids) {
        if (CollectionUtils.isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }

        return Q.New(TpmVO.class)
                .in(TpmVO_.vmInstanceUuid, resourceUuids)
                .select(TpmVO_.uuid)
                .listValues();
    }
}
