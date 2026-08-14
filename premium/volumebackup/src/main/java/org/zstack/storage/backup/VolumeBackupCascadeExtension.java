package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.volume.backup.VolumeBackupDeletionMsg;
import org.zstack.scheduler.SchedulerFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by kayo on 2018/7/25.
 */
public class VolumeBackupCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VolumeBackupCascadeExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private SchedulerFacade schedulerFacade;

    @Override
    public List<String> getEdgeNames() {
        return list(BackupStorageVO.class.getSimpleName());
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private List<VolumeBackupDeletionMsg> volumeBackupsDeletionMessagesFromAction(CascadeAction action) {
        return new SQLBatchWithReturn<List<VolumeBackupDeletionMsg>>() {

            @Override
            protected List<VolumeBackupDeletionMsg> scripts() {
                if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    List<AccountInventory> accounts = action.getParentIssuerContext();
                    List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

                    List<String> uuids = q(AccountResourceRefVO.class)
                            .select(AccountResourceRefVO_.resourceUuid)
                            .eq(AccountResourceRefVO_.resourceType, VolumeBackupVO.class.getSimpleName())
                            .in(AccountResourceRefVO_.accountUuid, accountUuids)
                            .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                            .listValues();

                    if (uuids.isEmpty()) {
                        return null;
                    }

                    return uuids.stream().map(uuid -> {
                        VolumeBackupDeletionMsg msg = new VolumeBackupDeletionMsg();
                        msg.setUuid(uuid);
                        msg.setDbOnly(true);
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, uuid);
                        return msg;
                    }).collect(Collectors.toList());
                } else if (BackupStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    List<BackupStorageInventory> backupStorages = action.getParentIssuerContext();
                    List<String> bsUuids = backupStorages.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList());

                    List<VolumeBackupDeletionMsg> msgs = new ArrayList<>();
                    for (String bsUuid : bsUuids) {
                        List<String> volumeBackupUuids = q(VolumeBackupStorageRefVO.class).select(VolumeBackupStorageRefVO_.volumeBackupUuid).eq(VolumeBackupStorageRefVO_.backupStorageUuid, bsUuid).listValues();

                        if (volumeBackupUuids.isEmpty()) {
                            continue;
                        }

                        for (String volumeBackupUuid : volumeBackupUuids) {
                            if (msgs.stream().anyMatch(msg -> msg.getUuid().equals(volumeBackupUuid) && msg.getBackupStorageUuids().add(bsUuid))) {
                                continue;
                            }

                            VolumeBackupDeletionMsg msg = new VolumeBackupDeletionMsg();
                            msg.setUuid(volumeBackupUuid);
                            List<String> backupStorageUuids = new ArrayList<>();
                            backupStorageUuids.add(bsUuid);
                            msg.setBackupStorageUuids(backupStorageUuids);
                            msg.setDbOnly(true);
                            bus.makeTargetServiceIdByResourceUuid(msg, VolumeBackupConstant.SERVICE_ID, volumeBackupUuid);
                            msgs.add(msg);
                        }
                    }

                    return msgs;
                } else {
                    return new ArrayList<>();
                }
            }
        }.execute();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<VolumeBackupDeletionMsg> msgs = volumeBackupsDeletionMessagesFromAction(action);

        if (msgs == null || msgs.isEmpty()) {
            completion.success();
            return;
        }
        new While<>(msgs).step((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete volume backup[uuid:%s], %s", msg.getUuid(), reply.getError()));
                }

                com.done();
            }
        }), 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public String getCascadeResourceName() {
        return VolumeBackupVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
