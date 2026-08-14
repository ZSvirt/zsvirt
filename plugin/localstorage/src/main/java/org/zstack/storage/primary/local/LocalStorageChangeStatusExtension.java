package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.*;

/**
 * Created by camile on 9/6/2017.
 */
public class LocalStorageChangeStatusExtension implements PrimaryStorageChangeStateExtensionPoint {

    private boolean isLocal(PrimaryStorageInventory inv) {
        return inv.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE);
    }

    @Override
    public void preChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState) throws PrimaryStorageException {
    }

    @Override
    public void beforeChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState nextState) {
    }

    @Override
    public void afterChangePrimaryStorageState(PrimaryStorageInventory inv, PrimaryStorageStateEvent evt, PrimaryStorageState previousState) {
        if (!isLocal(inv)) {
            return;
        }
        if (PrimaryStorageState.Enabled.equals(PrimaryStorageState.valueOf(inv.getState()))) {
            LocalStorageFactory.type.setSupportVolumeMigrationInCurrentPrimaryStorage(true);
        } else {
            LocalStorageFactory.type.setSupportVolumeMigrationInCurrentPrimaryStorage(false);
        }
    }
}
