package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.StaticInit;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerJobVO_;
import org.zstack.header.scheduler.SchedulerTriggerVO;
import org.zstack.header.scheduler.SchedulerVO;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeDeletionStruct;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SchedulerCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(SchedulerCascadeExtension.class);

    @Autowired
    private CloudBus bus;

    private static List<String> allowedVolumeDeletionPolicy = asList(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString()
            , VolumeDeletionPolicyManager.VolumeDeletionPolicy.DBOnly.toString());

    private static List<VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy> allowedVmDeletionPolicy = asList(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct
            , VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.DBOnly, VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.KeepVolume);

    private static List<String> cascadeUpdateTypes;

    @StaticInit
    static void staticInit() {
        cascadeUpdateTypes = new ArrayList<>(SchedulerJobParamCascadeUpdater.getResourceTypeForCascadeAction());
    }

    @Override
    public List<String> getEdgeNames() {
        List<String> types = new ArrayList<>(cascadeUpdateTypes);
        types.add(AccountVO.class.getSimpleName());
        types.add(VmInstanceVO.class.getSimpleName());
        types.add(VolumeVO.class.getSimpleName());
        return types;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void deleteSchedulerJobs(List<String> uuids, NoErrorCompletion completion) {
        List<SchedulerDeletionMsg> msgs = uuids.stream().map(auuid -> {
            SchedulerDeletionMsg msg = new SchedulerDeletionMsg();
            msg.setUuid(auuid);
            bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, auuid);
            return msg;
        }).collect(Collectors.toList());

        if (msgs == null || msgs.isEmpty()) {
            completion.done();
            return;
        }

        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete scheduler[uuid:%s], %s", msg.getUuid(), reply.getError()));
                }

                com.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void cascadeFromAccountDeletion(CascadeAction action, Completion completion) {
        List<AccountInventory> accounts = action.getParentIssuerContext();
        List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-scheduler");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-scheduler";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> uuids = ResourceHelper.findOwnResourceUuidList(SchedulerJobVO.class, accountUuids);
                        deleteSchedulerJobs(uuids, new NoErrorCompletion() {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-scheduler-trigger";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<SchedulerTriggerDeletionMsg> msgs = new SQLBatchWithReturn<List<SchedulerTriggerDeletionMsg>>() {
                            @Override
                            protected List<SchedulerTriggerDeletionMsg> scripts() {
                                List<String> pUuids = ResourceHelper.findOwnResourceUuidList(SchedulerTriggerVO.class, accountUuids);
                                if (pUuids.isEmpty()) {
                                    return null;
                                }

                                return pUuids.stream().map(auuid -> {
                                    SchedulerTriggerDeletionMsg msg = new SchedulerTriggerDeletionMsg();
                                    msg.setUuid(auuid);
                                    bus.makeTargetServiceIdByResourceUuid(msg, SchedulerConstant.SERVICE_ID, auuid);
                                    return msg;
                                }).collect(Collectors.toList());
                            }
                        }.execute();

                        if (msgs == null) {
                            trigger.next();
                            return;
                        }

                        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete scheduler trigger[uuid:%s], %s", msg.getUuid(), reply.getError()));
                                }

                                com.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private boolean issuedByZone(CascadeAction action) {
        return ZoneVO.class.getSimpleName().equals(action.getRootIssuer());
    }

    private List<String> jobFromTargetResourceDeleteAction(CascadeAction action) {
        if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VolumeDeletionStruct> structs = action.getParentIssuerContext();

            return new SQLBatchWithReturn<List<String>>() {
                @Override
                protected List<String> scripts() {
                    return structs.stream()
                            .filter(struct ->
                                    issuedByZone(action) ||
                                    (isAllowedDeletionPolicy(struct.getDeletionPolicy())
                                    && resourceExists(struct.getInventory().getUuid())))
                            .map(struct -> {
                                List<String> uuids = q(SchedulerJobVO.class)
                                        .select(SchedulerJobVO_.uuid)
                                        .eq(SchedulerJobVO_.targetResourceUuid, struct.getInventory().getUuid())
                                        .listValues();

                                return uuids;
                            }).flatMap(Collection::stream).collect(Collectors.toList());
                }

                private boolean isAllowedDeletionPolicy(String policy) {
                    return allowedVolumeDeletionPolicy.contains(policy);
                }

                private boolean resourceExists(String uuid) {
                    return q(SchedulerJobVO.class)
                            .eq(SchedulerJobVO_.targetResourceUuid, uuid)
                            .isExists();
                }
            }.execute();
        } else if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> structs = action.getParentIssuerContext();

            return new SQLBatchWithReturn<List<String>>() {
                @Override
                protected List<String> scripts() {
                    return structs.stream()
                            .filter(struct ->
                                    issuedByZone(action) ||
                                    (isAllowedDeletionPolicy(struct.getDeletionPolicy())
                                            && (resourceExists(struct.getInventory().getUuid())
                                    || resourceExists(struct.getInventory().getRootVolumeUuid()))))
                            .map(struct -> {
                                List<String> uuids = q(SchedulerJobVO.class)
                                        .select(SchedulerJobVO_.uuid)
                                        .in(SchedulerJobVO_.targetResourceUuid, asList(struct.getInventory().getUuid(), struct.getInventory().getRootVolumeUuid()))
                                        .listValues();

                                return uuids;
                            }).flatMap(Collection::stream).collect(Collectors.toList());
                }

                private boolean isAllowedDeletionPolicy(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy policy) {
                    return allowedVmDeletionPolicy.contains(policy);
                }

                private boolean resourceExists(String uuid) {
                    return q(SchedulerJobVO.class)
                            .eq(SchedulerJobVO_.targetResourceUuid, uuid)
                            .isExists();
                }
            }.execute();
        } else {
            return null;
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        if (cascadeUpdateTypes.contains(action.getParentIssuer())) {
            cascadeUpdateJob(action);
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            cascadeFromAccountDeletion(action, completion);
        } else if (VolumeVO.class.getSimpleName().equals(action.getParentIssuer())
                || VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> uuids = jobFromTargetResourceDeleteAction(action);

            if (uuids == null || uuids.isEmpty()) {
                completion.success();
                return;
            }

            deleteSchedulerJobs(uuids, new NoErrorCompletion() {
                @Override
                public void done() {
                    completion.success();
                }
            });
        } else {
            // cascadeUpdateTypes contains it
            completion.success();
        }
    }

    private void cascadeUpdateJob(CascadeAction action) {
        List<Object> invs = action.getParentIssuerContext();
        List<String> uuids = invs.stream().map(it -> JSONObjectUtil.rehashObject(it, ResourceInventory.class).getUuid()).collect(Collectors.toList());
        for (String uuid : uuids) {
            SchedulerJobParamCascadeUpdater.updateJobForResourceDeletion(uuid, action.getParentIssuer());
        }
    }

    @Override
    public String getCascadeResourceName() {
        return SchedulerVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
