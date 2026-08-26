package org.zstack.crypto.volumebackup;

import org.zstack.core.db.Q;
import org.zstack.header.keyprovider.KeyProviderRekeyAssociationExtensionPoint;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.backup.VolumeBackupVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class VolumeBackupRekeyAssociation implements KeyProviderRekeyAssociationExtensionPoint {
    @Override
    public String getType() {
        return VmInstanceVO.class.getSimpleName();
    }

    @Override
    public String getAssociatedResourceType() {
        return VolumeBackupVO.class.getSimpleName();
    }

    @Override
    public List<String> getAssociatedResourceUuids(List<String> resourceUuids) {
        if (CollectionUtils.isEmpty(resourceUuids)) {
            return Collections.emptyList();
        }

        return Q.New(VolumeBackupVO.class)
                .in(VolumeBackupVO_.vmInstanceUuid, resourceUuids)
                .select(VolumeBackupVO_.uuid)
                .listValues();
    }
}
