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
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.cbt.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
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
        FlowChain chain = new SimpleFlowChain();
        chain.setName("delete-cbt-task");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<DeleteCbtTaskMsg> msgs = cbtBackupsDeletionMessagesFromAction(action);
                logger.info(String.format("delete (%d) CBT tasks due to vm deletion", msgs.size()));
                if (msgs == null || msgs.isEmpty()) {
                    trigger.next();
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
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<UnexportNbdVolumesMsg> msgs = getUnexportVolumeMessagesFromAction(action);
                if (msgs == null || msgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(msgs).step((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to unexport volumes of vm[uuid:%s], %s", msg.getVmInstanceUuid(), reply.getError()));
                        }
                        com.done();
                    }
                }), 10).run(new WhileDoneCompletion(completion) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
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

    private List<UnexportNbdVolumesMsg> getUnexportVolumeMessagesFromAction(CascadeAction action) {
        return new SQLBatchWithReturn<List<UnexportNbdVolumesMsg>>() {

            @Override
            protected List<UnexportNbdVolumesMsg> scripts() {
                if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
                    List<String> vmUuids = ((List<VmDeletionStruct>) action.getParentIssuerContext()).stream().map(v -> v.getInventory().getUuid()).collect(Collectors.toList());

                    List<UnexportNbdVolumesMsg> msgs = new ArrayList<>();
                    for (String vmInstanceUuid : vmUuids) {
                        List<String> volumeUuids = q(VolumeVO.class)
                                .select(VolumeVO_.uuid)
                                .eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                                .listValues();

                        if (CollectionUtils.isEmpty(volumeUuids)) {
                            continue;
                        }

                        UnexportNbdVolumesMsg msg = new UnexportNbdVolumesMsg();
                        msg.setVmInstanceUuid(vmInstanceUuid);
                        msg.setVolumeUuids(volumeUuids);
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
