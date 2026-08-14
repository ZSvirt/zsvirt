package org.zstack.storage.cbt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.cbt.CbtTaskResourceRefVO;
import org.zstack.header.cbt.CbtTaskResourceRefVO_;
import org.zstack.header.cbt.CbtTaskVO;
import org.zstack.header.cbt.DeleteCbtTaskMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

public class CbtBackupCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(CbtBackupCascadeExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(VmInstanceVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return CbtTaskVO.class.getSimpleName();
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<DeleteCbtTaskMsg> msgs = cbtBackupsDeletionMessagesFromAction(action);
        logger.info(String.format("delete (%d) CBT tasks due to vm deletion", msgs.size()));

        if (msgs == null || msgs.isEmpty()) {
            completion.success();
            return;
        }

        new While<>(msgs).step((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete cbt backup[uuid:%s], %s", msg.getUuid(), reply.getError()));
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

    private List<DeleteCbtTaskMsg> cbtBackupsDeletionMessagesFromAction(CascadeAction action) {
        return new SQLBatchWithReturn<List<DeleteCbtTaskMsg>>() {

            @Override
            protected List<DeleteCbtTaskMsg> scripts() {
                if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    List<String> vmUuids = ((List<VmDeletionStruct>) action.getParentIssuerContext()).stream().map(v -> v.getInventory().getUuid()).collect(Collectors.toList());

                    List<DeleteCbtTaskMsg> msgs = new ArrayList<>();
                    for (String vmInstanceUuid : vmUuids) {
                        String cbtTaskUuid = q(CbtTaskResourceRefVO.class)
                                .eq(CbtTaskResourceRefVO_.resourceUuid, vmInstanceUuid)
                                .select(CbtTaskResourceRefVO_.taskUuid)
                                .findValue();

                        if (StringUtils.isEmpty(cbtTaskUuid)) {
                            continue;
                        }

                        DeleteCbtTaskMsg msg = new DeleteCbtTaskMsg();
                        msg.setUuid(cbtTaskUuid);
                        bus.makeLocalServiceId(msg, CbtBackupConstant.SERVICE_ID);
                        msgs.add(msg);
                    }

                    return msgs;
                } else {
                    return null;
                }
            }
        }.execute();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
