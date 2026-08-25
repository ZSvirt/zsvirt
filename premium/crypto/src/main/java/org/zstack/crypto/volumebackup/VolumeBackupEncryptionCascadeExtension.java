package org.zstack.crypto.volumebackup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.storage.volume.backup.VolumeBackupDeletionStruct;
import org.zstack.storage.encrypt.VolumeEncryptedResourceKeyBackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VolumeBackupEncryptionCascadeExtension extends AbstractCascadeExtension {
    private static final String NAME = EncryptedResourceKeyRefVO.class.getSimpleName();

    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;

    @Override
    public void syncCascade(CascadeAction action) {
        handleCascade(action);
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        handleCascade(action);
        completion.success();
    }

    private void handleCascade(CascadeAction action) {
        if (!action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)
                || !VolumeBackupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return;
        }

        List<String> backupUuids = volumeBackupUuidsFromAction(action);
        if (backupUuids.isEmpty()) {
            return;
        }

        for (String backupUuid : backupUuids) {
            volumeEncryptedResourceKeyBackend.detachKeyProviderFromBackup(backupUuid);
        }
    }

    private List<String> volumeBackupUuidsFromAction(CascadeAction action) {
        List<?> contexts = action.getParentIssuerContext();
        List<String> backupUuids = new ArrayList<>();
        if (contexts == null) {
            return backupUuids;
        }

        for (Object context : contexts) {
            if (context instanceof String) {
                backupUuids.add((String) context);
            } else if (context instanceof VolumeBackupDeletionStruct) {
                backupUuids.add(((VolumeBackupDeletionStruct) context).getVolumeBackupUuid());
            }
        }
        return backupUuids;
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(VolumeBackupVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
