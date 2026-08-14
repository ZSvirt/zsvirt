package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.cluster.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.UpdateHostOSMsg;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ClusterBase extends AbstractCluster {
    protected final static CLogger logger = Utils.getLogger(ClusterBase.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected ClusterExtensionPointEmitter extpEmitter;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ResourceConfigFacade rcf;

    protected ClusterVO self;
    protected final int threadSyncLevel = 1;

    public ClusterBase(ClusterVO self) {
        this.self = self;
    }

    protected ClusterInventory changeState(ClusterStateEvent event) {
        ClusterState currentState = self.getState();
        ClusterState next = AbstractCluster.getNextState(self.getState(), event);

        extpEmitter.beforeChange(self, event);
        self.setState(next);
        self = dbf.updateAndRefresh(self);
        ClusterInventory inv = ClusterInventory.valueOf(self);
        extpEmitter.afterChange(self, event, currentState);
        logger.debug("Cluster " + self.getName() + " uuid: " + self.getUuid() + " changed state from " + currentState + " to " + self.getState());
        return inv;
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg.getClass() == APIChangeClusterStateMsg.class) {
            handle((APIChangeClusterStateMsg) msg);
        } else if (msg.getClass() == APIDeleteClusterMsg.class) {
            handle((APIDeleteClusterMsg) msg);
        } else if (msg instanceof APIUpdateClusterMsg) {
            handle((APIUpdateClusterMsg) msg);
        } else if (msg instanceof APIUpdateClusterOSMsg) {
            handle((APIUpdateClusterOSMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateClusterOSMsg msg) {
        APIUpdateClusterOSEvent evt = new APIUpdateClusterOSEvent(msg.getId());

        // assemble jobData
        String jobData;
        String excludePackages;
        String updatePackages;
        String releaseVersion;
        excludePackages = msg.getExcludePackages() == null ? "" :String.join(",", msg.getExcludePackages());
        updatePackages = msg.getUpdatePackages() == null ? "" :String.join(",", msg.getUpdatePackages());
        releaseVersion = msg.getReleaseVersion() == null ? "" :msg.getReleaseVersion();
        jobData = String.format("{'uuid':'%s', 'excludePackages':'%s', 'updatePackages':'%s', 'releaseVersion':'%s'}",
                    msg.getUuid(), excludePackages, updatePackages, releaseVersion);

        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setJobName(APIUpdateClusterOSMsg.class.getSimpleName());
        smsg.setJobData(jobData);
        smsg.setResourceUuid(msg.getResourceUuid());
        smsg.setSystemTags(msg.getSystemTags());
        smsg.setUserTags(msg.getUserTags());
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                SubmitLongJobReply reply = rly.castReply();
                evt.setInventory(reply.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdateClusterMsg msg) {
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

        APIUpdateClusterEvent evt = new APIUpdateClusterEvent(msg.getId());
        evt.setInventory(ClusterInventory.valueOf(self));
        bus.publish(evt);
    }

    protected void handle(final APIDeleteClusterMsg msg) {
        final APIDeleteClusterEvent evt = new APIDeleteClusterEvent(msg.getId());
        final String issuer = ClusterVO.class.getSimpleName();
        final List<ClusterInventory> ctx = ClusterInventory.valueOf(Arrays.asList(self));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-cluster-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
                bus.publish(evt);
            }
        }).start();
    }

    protected void changeStateByApiMsg(APIChangeClusterStateMsg msg) {
        APIChangeClusterStateEvent evt = new APIChangeClusterStateEvent(msg.getId());
        ClusterStateEvent stateEvent = ClusterStateEvent.valueOf(msg.getStateEvent());
        try {
            extpEmitter.preChange(self, stateEvent);
        } catch (ClusterException e) {
            evt.setError(err(SysErrors.CHANGE_RESOURCE_STATE_ERROR, e.getMessage()));
            bus.publish(evt);
            return;
        }

        try {
            ClusterInventory inv = changeState(stateEvent);
            evt.setInventory(inv);
            bus.publish(evt);
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }

    }

    protected void handle(final APIChangeClusterStateMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "ChangeClusterStateByApi";
            }

            @Override
            public Void call() throws Exception {
                changeStateByApiMsg(msg);
                return null;
            }

            @Override
            public String getSyncSignature() {
                return self.getUuid();
            }

            @Override
            public int getSyncLevel() {
                return threadSyncLevel;
            }
        });
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof ChangeClusterStateMsg) {
            handle((ChangeClusterStateMsg) msg);
        } else if (msg instanceof ClusterDeletionMsg) {
            handle((ClusterDeletionMsg) msg);
        } else if (msg instanceof UpdateClusterOSMsg) {
            handle((UpdateClusterOSMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(UpdateClusterOSMsg msg) {
        UpdateClusterOSReply reply = new UpdateClusterOSReply();
        reply.setResults(new ConcurrentHashMap<>());

        ErrorCode error = extpEmitter.preUpdateOS(self);
        if (error != null) {
            reply.setError(error);
            bus.reply(msg, reply);
            return;
        }

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "update-cluster-os";
            }

            @Override
            public int getSyncLevel() {
                return ClusterGlobalConfig.CLUSTER_UPDATE_OS_PARALLELISM_DEGREE.value(Integer.class);
            }

            @Override
            public void run(SyncTaskChain chain) {
                extpEmitter.beforeUpdateOS(self);

                // update each hosts os in the cluster
                List<String> hostUuids = Q.New(HostVO.class)
                        .select(HostVO_.uuid)
                        .eq(HostVO_.clusterUuid, msg.getUuid())
                        .listValues();
                Boolean enableExpRepo = rcf.getResourceConfigValue(
                        ClusterGlobalConfig.ZSTACK_EXPERIMENTAL_REPO, msg.getUuid(), Boolean.class);
                new While<>(hostUuids)
                        .enableProgressReport("update-cluster-os-on-each-hosts")
                        .all((hostUuid, completion) -> {
                    UpdateHostOSMsg umsg = new UpdateHostOSMsg();
                    umsg.setUuid(hostUuid);
                    umsg.setClusterUuid(msg.getUuid());
                    umsg.setExcludePackages(msg.getExcludePackages());
                    umsg.setEnableExperimentalRepo(enableExpRepo);
                    umsg.setUpdatePackages(msg.getUpdatePackages());
                    umsg.setReleaseVersion(msg.getReleaseVersion());
                    bus.makeTargetServiceIdByResourceUuid(umsg, HostConstant.SERVICE_ID, hostUuid);
                    bus.send(umsg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply rly) {
                            if (rly.isSuccess()) {
                                reply.getResults().put(hostUuid, "success");
                            } else {
                                reply.getResults().put(hostUuid, rly.getError().getDetails());
                            }
                            completion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(msg, chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        extpEmitter.afterUpdateOS(self);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(ClusterDeletionMsg msg) {
        ClusterInventory inv = ClusterInventory.valueOf(self);
        extpEmitter.beforeDelete(inv);
        deleteHook();
        extpEmitter.afterDelete(inv);

        ClusterDeletionReply reply = new ClusterDeletionReply();
        bus.reply(msg ,reply);
    }

    protected void changeClusterStateByLocalMsg(ChangeClusterStateMsg msg) {
        ChangeClusterStateReply reply = new ChangeClusterStateReply();
        try {
            ClusterInventory inv = changeState(ClusterStateEvent.valueOf(msg.getStateEvent()));
            reply.setInventory(inv);
            bus.reply(msg, reply);
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }

    }

    protected void handle(final ChangeClusterStateMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public String getName() {
                return "ChangeClusterStateByLocalMessage";
            }

            @Override
            public Void call() throws Exception {
                changeClusterStateByLocalMsg(msg);
                return null;
            }

            @Override
            public String getSyncSignature() {
                return self.getUuid();
            }

            @Override
            public int getSyncLevel() {
                return threadSyncLevel;
            }
        });
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg instanceof APIMessage) {
                handleApiMessage((APIMessage) msg);
            } else {
                handleLocalMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg ,e);
        }
    }

    @Override
    protected void deleteHook() {
    }
}
