package org.zstack.scheduler.snapshot;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.*;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.snapshot.VolumeSnapshotGroupSystemTags.VOLUME_SNAPSHOT_GROUP_CREATED_BY_SYSTEM;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVolumeSnapshotGroupJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CreateVolumeSnapshotGroupJob.class);

    public static final String SCHEDULER_VOLUME_SNAPSHOT_GROUP_CREATED_PATH = "/scheduler/volume/snapshot-group-created";
    public static final String SCHEDULER_VOLUME_SNAPSHOT_GROUP_DELETED_PATH = "/scheduler/volume/snapshot-group-deleted";

    public static class VolumeSnapshotGroupCreatedData {
        public String jobUuid;
        public String schedulerName;
        public VolumeSnapshotGroupInventory snapshotGroup;

        private void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(SCHEDULER_VOLUME_SNAPSHOT_GROUP_CREATED_PATH, this);
        }
    }

    public static class VolumeSnapshotGroupDeletedData {
        public String jobUuid;
        public String schedulerName;
        public VolumeSnapshotGroupInventory snapshotGroup;

        private void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(SCHEDULER_VOLUME_SNAPSHOT_GROUP_DELETED_PATH, this);
        }
    }

    @Autowired
    private transient AccountManager acntMgr;
    @Autowired
    private transient SchedulerFacadeImpl schdlrf;
    @Autowired
    private transient EventFacade evtf;
    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient ThreadFacade thdf;
    @Autowired
    private transient DatabaseFacade dbf;
    @Autowired
    private transient TagManager tagMgr;

    private long snapshotGroupMaxNumber;

    void initFields(Map<String, String> parameters) {
        if (parameters != null && parameters.get(SchedulerJobParameters.snapshotGroupMax) != null) {
            this.snapshotGroupMaxNumber = Long.parseLong(parameters.get(SchedulerJobParameters.snapshotGroupMax));
        } else {
            this.snapshotGroupMaxNumber = 0;
        }
    }

    public CreateVolumeSnapshotGroupJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
        initFields(msg.getParameters());
    }

    public CreateVolumeSnapshotGroupJob() {
        super();
    }

    @Override
    public String getType() {
        return SchedulerType.VOLUME_SNAPSHOT_GROUP;
    }

    @Override
    public ErrorCode allowStateChange() {
        String vmUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, getTargetResourceUuid())
                .select(VolumeVO_.vmInstanceUuid).findValue();
        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .eq(VmInstanceVO_.state, VmInstanceState.Destroyed)
                .isExists()) {
            return operr("the vm of the root volume[%s] state in Destroyed. " +
                    "job state change is not allowed", getTargetResourceUuid());
        }
        return null;
    }

    @Override
    public CreateVolumeSnapshotGroupMsg buildRequest() {
        VolumeVO volumeVO = dbf.findByUuid(getTargetResourceUuid(), VolumeVO.class);
        if (volumeVO == null || volumeVO.getVmInstanceUuid() == null) {
            return null;
        }

        VmInstanceVO vmVO = dbf.findByUuid(volumeVO.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmVO == null) {
            return null;
        }
        List<VmInstanceState> states = Stream.of(VmInstanceState.Stopped, VmInstanceState.Paused, VmInstanceState.Running)
                .collect(Collectors.toList());
        if (!states.contains(vmVO.getState())) {
            return null;
        }

        CreateVolumeSnapshotGroupMsg cmsg = new CreateVolumeSnapshotGroupMsg();
        cmsg.setName(String.format("automatic-snapshot-group-%s-%s", vmVO.getName(), new Timestamp(System.currentTimeMillis())));
        cmsg.setRootVolumeUuid(getTargetResourceUuid());
        cmsg.setVmInstance(VmInstanceInventory.valueOf(vmVO));
        cmsg.setConsistentType(ConsistentType.None);
        SessionInventory session = new SessionInventory();
        session.setAccountUuid(getAccountUuid());
        cmsg.setSession(session);
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, cmsg.getVolumeUuid());
        return cmsg;
    }

    @Override
    public void execute(Object request, ReturnValueCompletion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("create-vm-%s-snapshot-group", getTargetResourceUuid());
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName("CreateVolumeSnapshotGroupJob");
                chain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        CreateVolumeSnapshotGroupMsg cmsg = (CreateVolumeSnapshotGroupMsg) request;
                        VmInstanceInventory vmInv = cmsg.getVmInstance();
                        final CreateVolumeSnapshotGroupReply[] createVolumeSnapshotGroupReply = new CreateVolumeSnapshotGroupReply[1];

                        flow(new NoRollbackFlow() {
                            String __name__ = "create-new-snapshot-group";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                bus.send((CreateVolumeSnapshotGroupMsg) request, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        SchedulerCanonicalEvents.SchedulerExecutedData data = new SchedulerCanonicalEvents.SchedulerExecutedData();
                                        data.setTargetResourceUuid(getTargetResourceUuid());
                                        data.setSchedulerName(getName());
                                        data.setJobUuid(getUuid());

                                        if (!reply.isSuccess()) {
                                            data.setError(reply.getError());
                                            data.setResultMessage(String.format("Create snapshot group of vm[uuid:%s] failed", getTargetResourceUuid()));
                                        } else {
                                            data.setResultMessage(String.format("Create snapshot group of vm[uuid:%s] succeed", getTargetResourceUuid()));
                                        }
                                        evtf.fire(SchedulerCanonicalEvents.VOLUME_SNAPSHOT_GROUP_SCHEDULER_PATH, data);

                                        if (!reply.isSuccess()) {
                                            trigger.fail(reply.getError());
                                            return;
                                        }

                                        createVolumeSnapshotGroupReply[0] = reply.castReply();
                                        createSystemTag(createVolumeSnapshotGroupReply[0].getInventory());
                                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                                            fireVfsCreatedDataEvent(createVolumeSnapshotGroupReply[0].getInventory());
                                        }
                                        trigger.next();
                                    }

                                    private void createSystemTag(VolumeSnapshotGroupInventory groupInv) {
                                        tagMgr.createNonInherentSystemTag(groupInv.getUuid(),
                                                VOLUME_SNAPSHOT_GROUP_CREATED_BY_SYSTEM.getTagFormat(),
                                                VolumeSnapshotGroupVO.class.getSimpleName());
                                        for (VolumeSnapshotGroupRefInventory ref : groupInv.getVolumeSnapshotRefs()) {
                                            tagMgr.createNonInherentSystemTag(ref.getVolumeSnapshotUuid(),
                                                    VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                                    VolumeSnapshotVO.class.getSimpleName());
                                        }
                                    }

                                    private void fireVfsCreatedDataEvent(VolumeSnapshotGroupInventory groupInv) {
                                        VolumeSnapshotGroupCreatedData createdEvent = new VolumeSnapshotGroupCreatedData();
                                        createdEvent.jobUuid = getUuid();
                                        createdEvent.schedulerName = getName();
                                        createdEvent.snapshotGroup = groupInv;
                                        createdEvent.fire();
                                    }
                                });
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "do-retention";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                doRetention(trigger);
                            }

                            private void doRetention(FlowTrigger trigger) {
                                List<VolumeSnapshotGroupVO> groupCreatedBySchedulerJobs = new SQLBatchWithReturn<List<VolumeSnapshotGroupVO>>() {
                                    @Override
                                    protected List<VolumeSnapshotGroupVO> scripts() {
                                        List<VolumeSnapshotGroupVO> groupCreatedBySchedulerJobs = sql(
                                                "select group from VolumeSnapshotGroupVO group, SystemTagVO sysTag where " +
                                                        "group.vmInstanceUuid = :vmUuid " +
                                                        "and sysTag.tag = :groupTag " +
                                                        "and group.uuid = sysTag.resourceUuid " +
                                                        "order by group.createDate desc", VolumeSnapshotGroupVO.class)
                                                .param("vmUuid", vmInv.getUuid())
                                                .param("groupTag", VOLUME_SNAPSHOT_GROUP_CREATED_BY_SYSTEM.getTagFormat())
                                                .list();

                                        int snapshotGroupCounts = groupCreatedBySchedulerJobs.size();
                                        if ((snapshotGroupMaxNumber == 0) || (snapshotGroupCounts <= snapshotGroupMaxNumber)) {
                                            return new ArrayList<>();
                                        }

                                        return groupCreatedBySchedulerJobs.subList((int) (snapshotGroupMaxNumber), snapshotGroupCounts);
                                    }
                                }.execute();

                                new While<>(groupCreatedBySchedulerJobs).each((group, whileCompletion) -> {
                                    DeleteVolumeSnapshotGroupInnerMsg dmsg = new DeleteVolumeSnapshotGroupInnerMsg();
                                    dmsg.setUuid(group.getUuid());
                                    dmsg.setDeletionMode(APIDeleteMessage.DeletionMode.Permissive);
                                    dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                                    dmsg.setDirection(DeleteVolumeSnapshotDirection.Auto.toString());
                                    bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, dmsg.getUuid());

                                    VolumeSnapshotGroupOverlayMsg omsg = new VolumeSnapshotGroupOverlayMsg();
                                    omsg.setVmInstanceUuid(vmInv.getUuid());
                                    omsg.setMessage(dmsg);
                                    bus.makeTargetServiceIdByResourceUuid(omsg, VmInstanceConstant.SERVICE_ID, vmInv.getUuid());
                                    bus.send(omsg, new CloudBusCallBack(whileCompletion) {
                                        @Override
                                        public void run(MessageReply reply) {
                                            if (!reply.isSuccess()) {
                                                logger.debug(String.format("CreateVolumeSnapshotGroupJob delete snapshot group failed " +
                                                        "because %s", reply.getError().getDetails()));
                                            } else {
                                                if (CoreGlobalProperty.UNIT_TEST_ON) {
                                                    fireVfsDeleteDateEvent(VolumeSnapshotGroupInventory.valueOf(group));
                                                }
                                            }
                                            whileCompletion.done();
                                        }
                                    });
                                }).run(new WhileDoneCompletion(trigger) {
                                    @Override
                                    public void done(ErrorCodeList errorCodeList) {
                                        trigger.next();
                                    }
                                });
                            }

                            private void fireVfsDeleteDateEvent(VolumeSnapshotGroupInventory groupInv) {
                                VolumeSnapshotGroupDeletedData createdEvent = new VolumeSnapshotGroupDeletedData();
                                createdEvent.jobUuid = getUuid();
                                createdEvent.schedulerName = getName();
                                createdEvent.snapshotGroup = groupInv;
                                createdEvent.fire();
                            }
                        });

                        done(new FlowDoneHandler(taskChain) {
                            @Override
                            public void handle(Map data) {
                                completion.success(createVolumeSnapshotGroupReply[0]);
                                taskChain.next();
                            }
                        });

                        error(new FlowErrorHandler(taskChain) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                completion.fail(errCode);
                                taskChain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public boolean lastJobIsRunning() {
        SchedulerJobHistoryVO schedulerJobHistoryVO = Q.New(SchedulerJobHistoryVO.class)
                .eq(SchedulerJobHistoryVO_.targetResourceUuid, getTargetResourceUuid())
                .eq(SchedulerJobHistoryVO_.jobType, SchedulerType.VOLUME_SNAPSHOT_GROUP)
                .orderBy(SchedulerJobHistoryVO_.startTime, SimpleQuery.Od.DESC)
                .limit(1).find();
        if (schedulerJobHistoryVO == null) {
            return false;
        }
        return schedulerJobHistoryVO.isRunning();
    }

    @Override
    public void updateSchedulerJob(Map<String, String> jobParameters) {
        super.updateSchedulerJob(jobParameters);
        mergeFields(jobParameters);
    }

    private void mergeFields(Map<String, String> parameters) {
        if (parameters == null) {
            return;
        }
        Optional.ofNullable(parameters.get("snapshotGroupMaxNumber")).ifPresent(it ->
                this.snapshotGroupMaxNumber = StringUtils.isBlank(it) ? 0 : Long.parseLong(it));
    }
}