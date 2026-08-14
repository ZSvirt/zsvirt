package org.zstack.compute.vmscheduling;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.affinityGroup.AffinityGroupFilterFlow;
import org.zstack.compute.affinityGroup.AffinityGroupManager;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.affinitygroup.AffinityGroupConstants;
import org.zstack.header.affinitygroup.AffinityGroupDeletionMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.PaginateCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vmscheduling.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2022/12/1
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostSchedulingRuleGroupBase {
    private static CLogger logger = Utils.getLogger(HostSchedulingRuleGroupBase.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected AffinityGroupFilterFlow filterFlow;
    @Autowired
    protected AffinityGroupManager agMgr;

    protected HostSchedulingRuleGroupVO self;

    public HostSchedulingRuleGroupBase(HostSchedulingRuleGroupVO self) {
        this.self = self;
    }

    private String getSyncId() {
        return String.format("host-scheduling-rule-group-%s", self.getUuid());
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateHostSchedulingRuleGroupMsg) {
            handle((APIUpdateHostSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIDeleteHostSchedulingRuleGroupMsg) {
            handle((APIDeleteHostSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIAddHostToHostSchedulingRuleGroupMsg) {
            handle((APIAddHostToHostSchedulingRuleGroupMsg) msg);
        } else if (msg instanceof APIDetachHostFromHostSchedulingRuleGroupMsg) {
            handle((APIDetachHostFromHostSchedulingRuleGroupMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDetachHostFromHostSchedulingRuleGroupMsg msg) {
        APIDetachHostFromHostSchedulingRuleGroupEvent evt = new APIDetachHostFromHostSchedulingRuleGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                UpdateQuery.New(HostSchedulingRuleGroupRefVO.class)
                        .eq(HostSchedulingRuleGroupRefVO_.hostUuid, msg.getHostUuid())
                        .eq(HostSchedulingRuleGroupRefVO_.hostGroupUuid, msg.getHostGroupUuid())
                        .hardDelete();
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIAddHostToHostSchedulingRuleGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIAddHostToHostSchedulingRuleGroupEvent event = new APIAddHostToHostSchedulingRuleGroupEvent(msg.getId());
                HostSchedulingRuleGroupRefVO refVO = new HostSchedulingRuleGroupRefVO();
                refVO.setHostGroupUuid(msg.getHostGroupUuid());
                refVO.setHostUuid(msg.getHostUuid());
                dbf.persistAndRefresh(refVO);
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIDeleteHostSchedulingRuleGroupMsg msg) {
        APIDeleteHostSchedulingRuleGroupEvent evt = new APIDeleteHostSchedulingRuleGroupEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteHostSchedulingRuleGroup(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void deleteHostSchedulingRuleGroup(APIDeleteHostSchedulingRuleGroupMsg msg, Completion completion) {
        FlowChain chain =  FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("remove-host-scheduling-rule-group-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("remove-vm-scheduling-rule-include-host-group-%s", self.getUuid());
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        long count = SQL.New("select count(distinct rule.uuid) from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.hostGroupUuid = :groupUuid")
                                .param("groupUuid", self.getUuid()).find();

                        SQL.New("select distinct rule.uuid from VmSchedulingRuleVO rule, VmSchedulingRuleRefVO ref " +
                                "where rule.uuid = ref.vmSchedulingRuleUuid and ref.hostGroupUuid = :groupUuid")
                                .param("groupUuid", self.getUuid()).limit(1000).paginate(count, (List<String> ruleUuids, PaginateCompletion paginateCompletion) -> {
                            new While<>(ruleUuids).each((ruleuuid, innerWhileCompletion) -> {
                                AffinityGroupDeletionMsg affinityGroupDeletionMsg = new AffinityGroupDeletionMsg();
                                affinityGroupDeletionMsg.setUuid(ruleuuid);
                                bus.makeTargetServiceIdByResourceUuid(affinityGroupDeletionMsg, AffinityGroupConstants.SERVICE_ID, ruleuuid);
                                bus.send(affinityGroupDeletionMsg, new CloudBusCallBack(innerWhileCompletion) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if(!reply.isSuccess()){
                                            logger.debug(String.format("delete vm scheduling rule[uuid:%s] failed ", ruleuuid));
                                        }
                                        innerWhileCompletion.done();
                                    }
                                });

                            }).run(new WhileDoneCompletion(paginateCompletion) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    paginateCompletion.done();
                                }
                            });
                        }, new NoErrorCompletion() {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        dbf.removeByPrimaryKey(self.getUuid(), HostSchedulingRuleGroupVO.class);
                        completion.success();
                    }
                });
            }
        }).start();
    }

    private void handle(APIUpdateHostSchedulingRuleGroupMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIUpdateHostSchedulingRuleGroupEvent event = new APIUpdateHostSchedulingRuleGroupEvent(msg.getId());
                boolean update = false;
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                    update = true;
                }

                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                    update = true;
                }

                if (update) {
                    self = dbf.updateAndRefresh(self);
                }

                event.setInventory(HostSchedulingRuleGroupInventory.valueOf(self));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getSyncSignature() {
                return getSyncId();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
}
