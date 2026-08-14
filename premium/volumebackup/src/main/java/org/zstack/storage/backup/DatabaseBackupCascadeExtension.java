package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.database.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class DatabaseBackupCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VolumeBackupCascadeExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(BackupStorageVO.class.getSimpleName());
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private List<DatabaseBackupDeletionMsg> actionMessageFromAction(CascadeAction action) {
        if (BackupStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<BackupStorageInventory> backupStorages = action.getParentIssuerContext();
            List<String> bsUuids = backupStorages.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList());

            return Q.New(DatabaseBackupStorageRefVO.class)
                    .in(DatabaseBackupStorageRefVO_.backupStorageUuid, bsUuids)
                    .select(DatabaseBackupStorageRefVO_.databaseBackupUuid)
                    .listValues().stream().distinct().map(it -> {
                        DatabaseBackupDeletionMsg msg = new DatabaseBackupDeletionMsg();
                        msg.setUuid(it.toString());
                        msg.setDbOnly(true);
                        msg.setBackupStorageUuids(bsUuids);
                        bus.makeTargetServiceIdByResourceUuid(msg, DatabaseBackupConstant.SERVICE_ID, it.toString());
                        return msg;
                    }).collect(Collectors.toList());

        } else {
            return new ArrayList<>();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<DatabaseBackupDeletionMsg> msgs = actionMessageFromAction(action);
        if (msgs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete database backup[uuid:%s] on bs[uuids:%s], %s",
                            msg.getUuid(), msg.getBackupStorageUuids(), reply.getError()));
                }

                com.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public String getCascadeResourceName() {
        return DatabaseBackupVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
