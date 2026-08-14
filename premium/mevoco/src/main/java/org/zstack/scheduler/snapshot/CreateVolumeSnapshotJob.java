package org.zstack.scheduler.snapshot;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
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
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.scheduler.*;
import org.zstack.header.scheduler.SchedulerCanonicalEvents.SchedulerExecutedData;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerFacadeImpl;
import org.zstack.scheduler.SchedulerJobParameters;
import org.zstack.scheduler.SchedulerType;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.snapshot.VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM_TOKEN;

/**
 * Created by Mei Lei on 7/11/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVolumeSnapshotJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CreateVolumeSnapshotJob.class);

    public static final String SCHEDULER_VOLUME_SNAPSHOT_CREATED_PATH = "/scheduler/volume/snapshot-created";
    public static final String SCHEDULER_VOLUME_SNAPSHOT_DELETED_PATH = "/scheduler/volume/snapshot-deleted";

    public static class SnapshotCreatedData {
        public String jobUuid;
        public String schedulerName;
        public VolumeSnapshotInventory snapshot;

        private void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(SCHEDULER_VOLUME_SNAPSHOT_CREATED_PATH, this);
        }
    }

    public static class SnapshotDeletedData {
        public String jobUuid;
        public String schedulerName;
        public VolumeSnapshotInventory snapshot;

        private void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(SCHEDULER_VOLUME_SNAPSHOT_DELETED_PATH, this);
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

    private long snapshotMaxNumber;

    public long getSnapshotMaxNumber() {
        return snapshotMaxNumber;
    }

    public void setSnapshotMaxNumber(long snapshotMaxNumber) {
        this.snapshotMaxNumber = snapshotMaxNumber;
    }

    void initFields(Map<String, String> parameters) {
        if (parameters != null && parameters.get(SchedulerJobParameters.snapshotMax) != null) {
            this.snapshotMaxNumber = Long.valueOf(parameters.get(SchedulerJobParameters.snapshotMax));
        } else {
            this.snapshotMaxNumber = 0;
        }
    }

    public CreateVolumeSnapshotJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
        initFields(msg.getParameters());
    }

    public CreateVolumeSnapshotJob() {
        super();
    }

    private void deleteOldSnapshots(FlowTrigger trigger, VolumeVO volumeVO){
        List<Consumer<WhileCompletion>> msgSenders = new SQLBatchWithReturn<List<Consumer<WhileCompletion>>> () {
            @Override
            protected List<Consumer<WhileCompletion>> scripts() {
                /* get ps type */
                String sql = "select ps.type from VolumeVO vol, PrimaryStorageVO ps " +
                        "where ps.uuid=vol.primaryStorageUuid and vol.uuid=:volUuid";
                String type = SQL.New(sql).param("volUuid", getTargetResourceUuid()).find();
                if (!type.equals(CephConstants.CEPH_PRIMARY_STORAGE_TYPE)) {
                    return null;
                }

                List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                        .eq(VolumeSnapshotVO_.volumeUuid, getTargetResourceUuid())
                        .orderBy(VolumeSnapshotVO_.createDate, SimpleQuery.Od.DESC).list();

                if (vos == null) {
                    return null;
                }

                List<String> snapshotsWithTag = Q.New(SystemTagVO.class)
                        .in(SystemTagVO_.resourceUuid, vos.stream().map(ResourceVO::getUuid).collect(Collectors.toList()))
                        .eq(SystemTagVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                        .eq(SystemTagVO_.tag, VOLUMESNAPSHOT_CREATED_BY_SYSTEM_TOKEN)
                        .select(SystemTagVO_.resourceUuid).listValues();

                vos.removeIf(vo -> !snapshotsWithTag.contains(vo.getUuid()));

                int snapshotCounts = vos.size();
                if ((snapshotMaxNumber == 0) || (snapshotCounts <= snapshotMaxNumber)) {
                    return null;
                }

                List<Consumer<WhileCompletion>> ret = new ArrayList<>();
                vos.subList((int) (snapshotMaxNumber), snapshotCounts).forEach(vo -> {
                    ret.add(whileCompletion -> {
                        VolumeSnapshotDeletionMsg msg = new VolumeSnapshotDeletionMsg();
                        msg.setSnapshotUuid(vo.getUuid());
                        msg.setTreeUuid(vo.getTreeUuid());
                        msg.setVolumeUuid(vo.getVolumeUuid());
                        msg.setVolumeDeletion(false);
                        msg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        msg.setDirection(DeleteVolumeSnapshotDirection.Auto.toString());
                        bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, vo.getVolumeUuid());

                        OverlayMessage omsg;
                        if (VolumeType.Root == volumeVO.getType()) {
                            omsg = new VolumeSnapshotDeletionOverlayVmMsg();
                            ((VolumeSnapshotDeletionOverlayVmMsg) omsg).setMessage(msg);
                            ((VolumeSnapshotDeletionOverlayVmMsg) omsg).setVmInstanceUuid(volumeVO.getVmInstanceUuid());
                            bus.makeTargetServiceIdByResourceUuid(omsg, VmInstanceConstant.SERVICE_ID, volumeVO.getVmInstanceUuid());
                        } else {
                            omsg = new VolumeSnapshotDeletionOverlayVolumeMsg();
                            ((VolumeSnapshotDeletionOverlayVolumeMsg) omsg).setMessage(msg);
                            ((VolumeSnapshotDeletionOverlayVolumeMsg) omsg).setVolumeUuid(volumeVO.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, volumeVO.getUuid());
                        }

                        bus.send(omsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.debug(String.format("CreateVolumeSnapshotJob delete first snapshot failed because %s", reply.getError().getDetails()));
                                } else {
                                    SnapshotDeletedData data = new SnapshotDeletedData();
                                    data.jobUuid = getUuid();
                                    data.schedulerName = getName();
                                    data.snapshot = vo.toInventory();
                                    data.fire();
                                }

                                whileCompletion.done();
                            }
                        });
                    });
                });

                return ret;
            }
        }.execute();

        if (msgSenders == null) {
            trigger.next();
            return;
        }

        new While<>(msgSenders).each((s, com) -> s.accept(com)).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                trigger.next();
            }
        });

    }

    private void createNewSnapshot(VolumeCreateSnapshotMsg cmsg, ReturnValueCompletion completion, FlowTrigger trigger, VolumeVO vo) {
        NeedReplyMessage msg = cmsg;

        /* root volume should be overlay on vm instance msg,
        data volume will be queue on volume queue, no need to overlay */
        if (VolumeType.Root == vo.getType()) {
            VolumeCreateSnapshotOverlayVmMsg overlayVmMsg = new VolumeCreateSnapshotOverlayVmMsg();
            overlayVmMsg.setMessage(cmsg);
            overlayVmMsg.setVmInstanceUuid(vo.getVmInstanceUuid());
            bus.makeTargetServiceIdByResourceUuid(overlayVmMsg, VmInstanceConstant.SERVICE_ID, vo.getVmInstanceUuid());
            msg = overlayVmMsg;
        }

        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                SchedulerExecutedData data = new SchedulerExecutedData();
                data.setTargetResourceUuid(getTargetResourceUuid());
                data.setSchedulerName(getName());
                data.setJobUuid(getUuid());

                if (reply.isSuccess()) {
                    data.setResultMessage(String.format("Create snap shot of volume[uuid:%s] succeed", getTargetResourceUuid()));
                } else {
                    data.setError(reply.getError());
                    data.setResultMessage(String.format("Create snap shot of volume[uuid:%s] failed", getTargetResourceUuid()));
                }

                evtf.fire(SchedulerCanonicalEvents.VOLUME_SNAPSHOT_SCHEDULER_PATH, data);

                if (reply.isSuccess()) {
                    VolumeCreateSnapshotReply r = reply.castReply();
                    SnapshotCreatedData sdata = new SnapshotCreatedData();
                    sdata.schedulerName = getName();
                    sdata.jobUuid = getUuid();
                    sdata.snapshot = r.getInventory();
                    sdata.fire();

                    tagMgr.createNonInherentSystemTag(r.getInventory().getUuid(),
                            VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                            VolumeSnapshotVO.class.getSimpleName());

                    completion.success(reply);
                    trigger.next();
                } else {
                    completion.fail(reply.getError());
                    trigger.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public VolumeCreateSnapshotMsg buildRequest() {
        logger.debug(String.format("run scheduler for job: CreateVolumeSnapshotJob; volume uuid is %s", getTargetResourceUuid()));
        VolumeVO vo = dbf.findByUuid(getTargetResourceUuid(), VolumeVO.class);

        if (VolumeStatus.Migrating == vo.getStatus()) {
            return null;
        }

        if (vo.getVmInstanceUuid() != null) {
            VmInstanceVO vmVo = dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class);
            if (VmInstanceState.Migrating == vmVo.getState() || VmInstanceState.VolumeMigrating == vmVo.getState()) {
                return null;
            }
        }

        VolumeCreateSnapshotMsg cmsg = new VolumeCreateSnapshotMsg();
        cmsg.setName(getTargetResourceUuid() + "-snapshot-" + new Timestamp(System.currentTimeMillis()).toString());
        cmsg.setVolumeUuid(getTargetResourceUuid());
        cmsg.setAccountUuid(acntMgr.getOwnerAccountUuidOfResource(getTargetResourceUuid()));
        cmsg.setTimeout(TimeUnit.HOURS.toMillis(3));
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, getTargetResourceUuid());
        return cmsg;
    }

    @Override
    public void execute(Object request, ReturnValueCompletion completion) {
        VolumeVO vo = dbf.findByUuid(getTargetResourceUuid(), VolumeVO.class);
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("create-snapshot-%s", getTargetResourceUuid());
            }

            @Override
            public void run(SyncTaskChain taskChain) {
                FlowChain chain = FlowChainBuilder.newShareFlowChain();
                chain.setName("CreateVolumeSnapshotJob");
                chain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = "create-new-snapshot";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                createNewSnapshot((VolumeCreateSnapshotMsg) request, completion, trigger, vo);
                            }
                        });

                        flow(new NoRollbackFlow() {
                            String __name__ = "delete-old-snapshot";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                deleteOldSnapshots(trigger, vo);
                            }
                        });

                        done(new FlowDoneHandler(taskChain) {
                            @Override
                            public void handle(Map data) {
                                taskChain.next();
                            }
                        });

                        error(new FlowErrorHandler(taskChain) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
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
    public ErrorCode allowStateChange() {
        if (Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, getTargetResourceUuid())
                .eq(VolumeVO_.status, VolumeStatus.Deleted)
                .isExists()) {
            return operr("volume[uuid:%s] is deleted, state change is not allowed", getTargetResourceUuid());
        }

        return null;
    }

    @Override
    public String getType() {
        return SchedulerType.VOLUME_SNAPSHOT;
    }

    @Override
    public boolean lastJobIsRunning() {
        SchedulerJobHistoryVO schedulerJobHistoryVO = Q.New(SchedulerJobHistoryVO.class)
                .eq(SchedulerJobHistoryVO_.targetResourceUuid, getTargetResourceUuid())
                .eq(SchedulerJobHistoryVO_.jobType, SchedulerType.VOLUME_SNAPSHOT)
                .orderBy(SchedulerJobHistoryVO_.startTime, SimpleQuery.Od.DESC)
                .limit(1).find();
        if (schedulerJobHistoryVO == null) {
            return false;
        }
        return schedulerJobHistoryVO.isRunning();
    }
}
