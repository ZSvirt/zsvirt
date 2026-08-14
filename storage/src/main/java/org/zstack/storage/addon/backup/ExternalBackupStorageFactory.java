package org.zstack.storage.addon.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.backup.APIAddExternalBackupStorageMsg;
import org.zstack.header.storage.addon.backup.BackupStorageController;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageInventory;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageVO;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ExternalBackupStorageFactory implements BackupStorageFactory, Component {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorageFactory.class);
    public static BackupStorageType type = new BackupStorageType(BackupStorageConstant.EXTERNAL_BACKUP_STORAGE_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    static {
        type.setOrder(999);
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    public BackupStorageInventory createBackupStorage(BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddExternalBackupStorageMsg amsg = (APIAddExternalBackupStorageMsg) msg;
        if (findBackupStorageController(amsg.getIdentity()) == null) {
            throw new OperationFailureException(
                    Platform.operr("No backup storage plugin registered with identity: %s", amsg.getIdentity())
            );
        }

        final ExternalBackupStorageVO lvo = new ExternalBackupStorageVO(vo);
        lvo.setIdentity(amsg.getIdentity());
        dbf.persist(lvo);
        return ExternalBackupStorageInventory.valueOf(lvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        ExternalBackupStorageVO lvo = dbf.findByUuid(vo.getUuid(), ExternalBackupStorageVO.class);
        BackupStorageController c = findBackupStorageController(lvo.getIdentity());
        if (c == null) {
            throw new OperationFailureException(
                    Platform.operr("No backup storage plugin registered with identity: %s", vo.getType())
            );
        }
        return new ExternalBackupStorage(lvo, c);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        ExternalBackupStorageVO vo = dbf.findByUuid(uuid, ExternalBackupStorageVO.class);
        return ExternalBackupStorageInventory.valueOf(vo);
    }

    // TODO allow reload upon request and deduplication
    private BackupStorageController findBackupStorageController(String identity) {
        for (BackupStorageController c : pluginRgty.getExtensionList(BackupStorageController.class)) {
            if (c.getIdentity().equals(identity)) {
                return c;
            }
        }

        return null;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
