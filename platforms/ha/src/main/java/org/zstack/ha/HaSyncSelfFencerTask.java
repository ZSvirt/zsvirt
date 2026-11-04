package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.ha.entity.HostHaStateVO;
import org.zstack.ha.entity.HostHaStateVO_;
import org.zstack.ha.header.HaConstants;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.HostHaState;
import org.zstack.ha.header.HostHaStateEvent;
import org.zstack.ha.message.GetFencerStateFromHostMsg;
import org.zstack.ha.message.RetryCancelSelfFencerOnHostMsg;
import org.zstack.ha.message.RetrySetupSelfFencerOnHostMsg;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HaSyncSelfFencerTask implements PeriodicTask {
    private static final CLogger logger = Utils.getLogger(HaSyncSelfFencerTask.class);

    private int interval;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    public HaSyncSelfFencerTask(int interval) {
        this.interval = interval;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public String getName() {
        return HaScheduler.TASK_NAME_REFRESH_FENCERS;
    }

    @Override
    public void run() {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public void run(SyncTaskChain chain) {
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName("ha-sync-state-retry-setup-cancel-self-fencer-flow");
                fchain.then(new Flow() {
                    String __name__ = "ha-sync-self-fencer-state";

                    private void syncSelfFencerStateFromHost(String uuid, WhileCompletion completion) {
                        HostVO hostVo = checkHost(uuid);
                        if (hostVo == null) {
                            completion.done();
                            return;
                        }

                        HostInventory inv = HostInventory.valueOf(hostVo);
                        GetFencerStateFromHostMsg msg = new GetFencerStateFromHostMsg();
                        msg.setHostInventory(inv);

                        bus.makeTargetServiceIdByResourceUuid(msg, HaConstants.SERVICE_ID, inv.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to sync self-fencer state on host[uuid:%s]", uuid));
                                }
                                completion.done();
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map<String, Object> data) {
                        List<String> uuids = Q.New(HostHaStateVO.class)
                                .select(HostHaStateVO_.hostUuid)
                                .listValues();
                        if (uuids.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        uuids = uuids.stream().distinct().collect(Collectors.toList());

                        new While<>(uuids)
                                .each(this::syncSelfFencerStateFromHost)
                                .run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (!errorCodeList.getCauses().isEmpty()) {
                                            logger.warn(String.format("failed to sync self-fencer state from host: %s", errorCodeList.getCauses()));
                                        }
                                        trigger.next();
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map<String, Object> data) {
                        trigger.rollback();
                    }
                }).then(new Flow() {
                    String __name__ = "ha-retry-setup-self-fencer";

                    @Override
                    public boolean skip(Map<String, Object> data) {
                        if (!HaHelper.isVmHaEnabled()) {
                            logger.debug(String.format("the global config[%s] is set to false, skip retry setting up self-fencer on the failed hosts",
                                    HaGlobalConfig.ALL.getCanonicalName()));
                            return true;
                        }
                        return false;
                    }

                    private void reSetupSelfFencerOnHost(String uuid, WhileCompletion completion) {
                        HostVO hostVo = checkHost(uuid);
                        if (hostVo == null) {
                            completion.done();
                            return;
                        }

                        List<HostHaStateVO> hostHaStateVOs = Q.New(HostHaStateVO.class)
                                .eq(HostHaStateVO_.state, HostHaState.SetupFail)
                                .eq(HostHaStateVO_.hostUuid, uuid)
                                .list();
                        if (hostHaStateVOs.isEmpty()) {
                            completion.done();
                            return;
                        }
                        List<String> claimedPsUuids = new ArrayList<>();
                        for (HostHaStateVO hostHaStateVO : hostHaStateVOs) {
                            int updated = SQL.New(HostHaStateVO.class)
                                    .eq(HostHaStateVO_.id, hostHaStateVO.getId())
                                    .eq(HostHaStateVO_.state, HostHaState.SetupFail)
                                    .set(HostHaStateVO_.state, HostHaStateMachine.getState(hostHaStateVO.getState(), HostHaStateEvent.setup))
                                    .update();
                            if (updated > 0) {
                                claimedPsUuids.add(hostHaStateVO.getPrimaryStorageUuid());
                            }
                        }
                        if (claimedPsUuids.isEmpty()) {
                            completion.done();
                            return;
                        }

                        HostInventory inv = HostInventory.valueOf(hostVo);
                        RetrySetupSelfFencerOnHostMsg msg = new RetrySetupSelfFencerOnHostMsg();
                        msg.setHostInventory(inv);
                        msg.setPrimaryStorageUuids(claimedPsUuids);

                        bus.makeTargetServiceIdByResourceUuid(msg, HaConstants.SERVICE_ID, inv.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to retry setting up self-fencer on host[uuid:%s]", uuid));
                                }
                                completion.done();
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map<String, Object> data) {
                        List<String> uuids = Q.New(HostHaStateVO.class)
                                .select(HostHaStateVO_.hostUuid)
                                .eq(HostHaStateVO_.state, HostHaState.SetupFail)
                                .listValues();
                        if (uuids.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        uuids = uuids.stream().distinct().collect(Collectors.toList());

                        new While<>(uuids)
                                .each(this::reSetupSelfFencerOnHost)
                                .run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (!errorCodeList.getCauses().isEmpty()) {
                                            logger.warn(String.format("failed to retry setting up self-fencer on host: %s", errorCodeList.getCauses()));
                                        }
                                        trigger.next();
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map<String, Object> data) {
                        trigger.rollback();
                    }
                }).then(new Flow() {
                    String __name__ = "ha-retry-cancel-self-fencer";

                    private void reCancelSelfFencerOnHost(String uuid, WhileCompletion completion) {
                        HostVO hostVo = checkHost(uuid);
                        if (hostVo == null) {
                            completion.done();
                            return;
                        }

                        List<HostHaStateVO> hostHaStateVOs = Q.New(HostHaStateVO.class)
                                .eq(HostHaStateVO_.state, HostHaState.CancelFail)
                                .eq(HostHaStateVO_.hostUuid, uuid)
                                .list();
                        if (hostHaStateVOs.isEmpty()) {
                            completion.done();
                            return;
                        }
                        for (HostHaStateVO hostHaStateVO : hostHaStateVOs) {
                            hostHaStateVO.setState(HostHaStateMachine.getState(hostHaStateVO.getState(), HostHaStateEvent.cancel));
                            dbf.update(hostHaStateVO);
                        }

                        HostInventory inv = HostInventory.valueOf(hostVo);
                        RetryCancelSelfFencerOnHostMsg msg = new RetryCancelSelfFencerOnHostMsg();
                        msg.setHostInventory(inv);
                        msg.setPrimaryStorageUuids(hostHaStateVOs.stream().map(HostHaStateVO::getPrimaryStorageUuid).collect(Collectors.toList()));

                        bus.makeTargetServiceIdByResourceUuid(msg, HaConstants.SERVICE_ID, inv.getUuid());
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to retry cancel self-fencer on host[uuid:%s]", uuid));
                                }
                                completion.done();
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map<String, Object> data) {
                        List<String> uuids = Q.New(HostHaStateVO.class)
                                .select(HostHaStateVO_.hostUuid)
                                .eq(HostHaStateVO_.state, HostHaState.CancelFail)
                                .listValues();
                        if (uuids.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        uuids = uuids.stream().distinct().collect(Collectors.toList());

                        new While<>(uuids)
                                .each(this::reCancelSelfFencerOnHost)
                                .run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        if (!errorCodeList.getCauses().isEmpty()) {
                                            logger.warn(String.format("failed to retry cancel self-fencer on host: %s", errorCodeList.getCauses()));
                                        }
                                        trigger.next();
                                    }
                                });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map<String, Object> data) {
                        trigger.rollback();
                    }
                }).done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map<String, Object> data) {
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map<String, Object> data) {
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public String getName() {
                return "ha-sync-state-retry-setup-cancel-self-fencer-task-chaintask";
            }

            @Override
            protected int getMaxPendingTasks() {
                return 1;
            }

            @Override
            protected String getDeduplicateString() {
                return getSyncSignature();
            }
        });
    }

    private HostVO checkHost(String uuid) {
        if (!destinationMaker.isManagedByUs(uuid)) {
            return null;
        }
        HostVO hostVo = Q.New(HostVO.class)
                .eq(HostVO_.uuid, uuid)
                .find();
        if (hostVo == null) {
            logger.debug(String.format("skip retry setting up self-fencer because host[%s] do not exist in database", uuid));
            return null;
        }
        if (HostStatus.Connected != hostVo.getStatus()) {
            logger.debug(String.format("skip retry setting up self-fencer because host[%s] status is [%s]", uuid, hostVo.getStatus()));
            return null;
        }
        if (HostState.Enabled != hostVo.getState()) {
            logger.debug(String.format("skip retry setting up self-fencer because host[%s] state is [%s]", uuid, hostVo.getState()));
            return null;
        }
        return hostVo;
    }
}
