package org.zstack.storage.primary.local;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.VolumeMigrationTargetHostFilter;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.*;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.OverlayMessage;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CleanupVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CleanupVmInstanceMetadataOnPrimaryStorageReply;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageMsg;
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataOnPrimaryStorageReply;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.storage.primary.*;
import org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply.HostDiskCapacity;
import org.zstack.storage.primary.local.MigrateBitsStruct.ResourceInfo;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.storage.primary.local.LocalStorageUtils.getHostUuidFromInstallUrl;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.toMap;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(LocalStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    private LocalStorageImageCleaner imageCacheCleaner;
    @Autowired
    private EventFacade eventf;
    @Autowired
    private LocalStorageHostUsageReport localForecaster;

    static class FactoryCluster {
        LocalStorageHypervisorFactory factory;
        List<ClusterInventory> clusters;
    }

    public LocalStorageBase() {
    }

    public LocalStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.detachHook(clusterUuid, new Completion(completion) {
            @Override
            public void success() {
                syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(null) {
                    @Override
                    public void success(PhysicalCapacityUsage returnValue) {
                        setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to sync the physical capacity on the local primary storage[uuid:%s], %s",
                                self.getUuid(), errorCode));
                    }
                });

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetLocalStorageHostDiskCapacityMsg) {
            handle((APIGetLocalStorageHostDiskCapacityMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            handle((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APILocalStorageGetVolumeMigratableHostsMsg) {
            handle((APILocalStorageGetVolumeMigratableHostsMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(APILocalStorageGetVolumeMigratableHostsMsg msg) {
        // this API does the best it can to find migratable hosts.
        // it doesn't count the base image size because the image may have
        // been deleted, and ZStack has to consult the host for the image size

        APILocalStorageGetVolumeMigratableReply reply = new APILocalStorageGetVolumeMigratableReply();

        new SQLBatch() {
            @Override
            protected void scripts() {

                //1.count the image size of volume
                long size = SQL.New("select vol.size" +
                        " from VolumeVO vol" +
                        " where vol.uuid = :uuid")
                        .param("uuid", msg.getVolumeUuid()).find();
                size = ratioMgr.calculateByRatio(self.getUuid(), size);


                Long snapshotSize = SQL.New("select sum(sp.size)" +
                        " from VolumeSnapshotVO sp" +
                        " where sp.volumeUuid = :volUuid")
                        .param("volUuid", msg.getVolumeUuid()).find();
                if (snapshotSize != null) {
                    size += snapshotSize;
                }


                //2.select hosts that have enough capacity
                double physicalThreshold = physicalCapacityMgr.getRatio(self.getUuid());
                List<String> hostUuids = SQL.New("select href.hostUuid" +
                        " from LocalStorageHostRefVO href" +
                        " where href.hostUuid !=" +
                        " (" +
                        " select rref.hostUuid" +
                        " from LocalStorageResourceRefVO rref" +
                        " where rref.resourceUuid = :volUuid" +
                        " and rref.resourceType = :rtype" +
                        " )" +
                        " and (href.totalPhysicalCapacity * (1.0 - :thres)) <= href.availablePhysicalCapacity" +
                        " and href.availablePhysicalCapacity != 0" +
                        " and href.availableCapacity >= :size" +
                        " and href.primaryStorageUuid = :psUuid" +
                        " group by href.hostUuid")
                        .param("volUuid", msg.getVolumeUuid())
                        .param("rtype", VolumeVO.class.getSimpleName())
                        .param("thres", physicalThreshold)
                        .param("size", size)
                        .param("psUuid", self.getUuid()).list();

                if (hostUuids.isEmpty()) {
                    reply.setInventories(new ArrayList<HostInventory>());
                    bus.reply(msg, reply);
                    return;
                }

                List<HostVO> hosts = new LinkedList<>(SQL.New("select h from HostVO h " +
                        " where h.uuid in (:uuids)" +
                        " and h.status = :hstatus")
                        .param("uuids", hostUuids)
                        .param("hstatus", HostStatus.Connected).list());

                //3.check if the network environment meets the requirement of vm running after migrate When migrate the rootVolume
                boolean isRootVolume = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.uuid, msg.getVolumeUuid())
                        .eq(VolumeVO_.type, VolumeType.Root)
                        .isExists();
                if (isRootVolume) {
                    Tuple tuple = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.clusterUuid, VmInstanceVO_.uuid)
                            .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid()).findTuple();
                    String originClusterUuid = tuple.get(0, String.class);
                    String originVmUuid = tuple.get(1, String.class);
                    if (originClusterUuid == null) {
                        throw new ApiMessageInterceptionException(
                                err(SysErrors.INTERNAL, "The clusterUuid of vm cannot be null when migrate the vm"));
                    }


                    Iterator<HostVO> it = hosts.iterator();
                    while (it.hasNext()) {
                        HostVO hostVO = it.next();
                        String destClusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                                .eq(HostVO_.uuid, hostVO.getUuid()).findValue();
                        if (!originClusterUuid.equals(destClusterUuid)) {
                            List<String> originL2NetworkList = sql("select l2NetworkUuid from L3NetworkVO" +
                                    " where uuid in(select l3NetworkUuid from VmNicVO where vmInstanceUuid = :vmUuid)")
                                    .param("vmUuid", originVmUuid).list();
                            List<String> l2NetworkList = sql("select l2NetworkUuid from L2NetworkClusterRefVO" +
                                    " where clusterUuid = :clusterUuid")
                                    .param("clusterUuid", destClusterUuid).list();

                            for (String l2 : originL2NetworkList) {
                                if (!l2NetworkList.contains(l2)) {
                                    //remove inappropriate host from list
                                    it.remove();
                                    break;
                                }
                            }
                        }
                    }
                }
                List<VolumeMigrationTargetHostFilter> exts = pluginRgty.getExtensionList(VolumeMigrationTargetHostFilter.class);
                for (VolumeMigrationTargetHostFilter hostFilter : exts) {
                    hosts = hostFilter.filter(hosts);
                }
                reply.setInventories(HostInventory.valueOf(hosts));
            }
        }.execute();

        bus.reply(msg, reply);
    }

    private void handle(final APILocalStorageMigrateVolumeMsg msg) {
        final APILocalStorageMigrateVolumeEvent evt = new APILocalStorageMigrateVolumeEvent(msg.getId());

        if (self.getState() == PrimaryStorageState.Disabled) {
            evt.setError(operr("The primary storage[uuid:%s] is disabled cold migrate is not allowed", msg.getPrimaryStorageUuid()));
            bus.publish(evt);
            return;
        }

        class MigrateStruct {
            private boolean isRootVolume = false;
            private OverlayMessage message;
            private String vmUuid;
            private boolean volumeStatusChanged = false;
            private boolean vmStateChanged = false;
            private String vmOriginStateDrivenEvt;

            public OverlayMessage getMessage() {
                return message;
            }

            public void setMessage(OverlayMessage message) {
                this.message = message;
            }

            public String getVmUuid() {
                return vmUuid;
            }

            public void setVmUuid(String vmUuid) {
                this.vmUuid = vmUuid;
            }

            public boolean isVolumeStatusChanged() {
                return volumeStatusChanged;
            }

            public void setVolumeStatusChanged(boolean volumeStatusChanged) {
                this.volumeStatusChanged = volumeStatusChanged;
            }

            public boolean isVmStateChanged() {
                return vmStateChanged;
            }

            public void setVmStateChanged(boolean vmStateChanged) {
                this.vmStateChanged = vmStateChanged;
            }
        }

        MigrateStruct struct = new MigrateStruct();
        VolumeStatus originStatus = Q.New(VolumeVO.class).select(VolumeVO_.status).eq(VolumeVO_.uuid, msg.getVolumeUuid()).findValue();
        String lastHostUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.lastHostUuid).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("local-storage-%s-migrate-volume-%s-to-host-%s", msg.getPrimaryStorageUuid(), msg.getVolumeUuid(), msg.getDestHostUuid()));
        chain.then(new Flow() {
            String __name__ = "change-volume-status-to-migrating";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ChangeVolumeStatusMsg changeVolumeStatusMsg = new ChangeVolumeStatusMsg();
                changeVolumeStatusMsg.setStatus(VolumeStatus.Migrating);
                changeVolumeStatusMsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(changeVolumeStatusMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                bus.send(changeVolumeStatusMsg, new CloudBusCallBack(changeVolumeStatusMsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        struct.setVolumeStatusChanged(true);
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (struct.isVolumeStatusChanged()) {
                    ChangeVolumeStatusMsg rollbackMsg = new ChangeVolumeStatusMsg();
                    rollbackMsg.setStatus(originStatus);
                    rollbackMsg.setVolumeUuid(msg.getVolumeUuid());
                    bus.makeTargetServiceIdByResourceUuid(rollbackMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                    bus.send(rollbackMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.rollback();
                        }
                    });
                } else {
                    trigger.rollback();
                }
            }
        }).then(new Flow() {
            String __name__ = "change-vm-state-to-volume-migrating";

            @Override
            public boolean skip(Map data) {
                Tuple t = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                        .eq(VmInstanceVO_.rootVolumeUuid, msg.getVolumeUuid())
                        .findTuple();
                struct.vmUuid = t == null ? null : t.get(0, String.class);
                struct.vmOriginStateDrivenEvt = t == null ? null : t.get(1, VmInstanceState.class).getDrivenEvent().toString();
                struct.isRootVolume = struct.vmUuid != null;
                return !struct.isRootVolume;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrating.toString());
                cmsg.setVmInstanceUuid(struct.getVmUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, struct.getVmUuid());
                bus.send(cmsg, new CloudBusCallBack(cmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        struct.setVmStateChanged(true);
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (struct.isVmStateChanged()) {
                    ChangeVmStateMsg rollbackMsg = new ChangeVmStateMsg();
                    rollbackMsg.setStateEvent(struct.vmOriginStateDrivenEvt);
                    rollbackMsg.setVmInstanceUuid(struct.getVmUuid());
                    bus.makeTargetServiceIdByResourceUuid(rollbackMsg, VmInstanceConstant.SERVICE_ID, struct.getVmUuid());
                    bus.send(rollbackMsg, new CloudBusCallBack(trigger) {
                        @Override
                        public void run(MessageReply reply) {
                            trigger.rollback();
                        }
                    });
                } else {
                    trigger.rollback();
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "migrate-volume-on-local-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                MigrateVolumeOnLocalStorageMsg mmsg = new MigrateVolumeOnLocalStorageMsg();
                mmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                mmsg.setDestHostUuid(msg.getDestHostUuid());
                mmsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());

                MigrateVolumeOverlayMsg omsg = new MigrateVolumeOverlayMsg();
                omsg.setMessage(mmsg);
                omsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());

                struct.setMessage(omsg);

                if (struct.isRootVolume) {
                    MigrateRootVolumeVmOverlayMsg vmsg = new MigrateRootVolumeVmOverlayMsg();
                    vmsg.setMessage(omsg);
                    vmsg.setVmInstanceUuid(struct.getVmUuid());
                    bus.makeTargetServiceIdByResourceUuid(vmsg, VmInstanceConstant.SERVICE_ID, struct.getVmUuid());

                    struct.setMessage(vmsg);
                }

                bus.send(struct.getMessage(), new CloudBusCallBack(struct.getMessage()) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        MigrateVolumeOnLocalStorageReply mr = reply.castReply();
                        evt.setInventory(mr.getInventory());
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-vm-state-to-volume-migrated";

            @Override
            public boolean skip(Map data) {
                return !struct.isRootVolume;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setStateEvent(VmInstanceStateEvent.volumeMigrated.toString());
                cmsg.setVmInstanceUuid(struct.getVmUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, struct.getVmUuid());
                // if fail, host ping task will sync it state
                bus.send(cmsg, new CloudBusCallBack(cmsg) {
                    @Override
                    public void run(MessageReply reply) {
                        // due to the vm sync will set it back, next
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-volume-status-to-origin";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ChangeVolumeStatusMsg changeVolumeStatusMsg = new ChangeVolumeStatusMsg();
                changeVolumeStatusMsg.setStatus(originStatus);
                changeVolumeStatusMsg.setVolumeUuid(msg.getVolumeUuid());
                bus.makeTargetServiceIdByResourceUuid(changeVolumeStatusMsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                bus.send(changeVolumeStatusMsg, new CloudBusCallBack(changeVolumeStatusMsg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                /* update vm last host uuid */
                SQL.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, struct.getVmUuid())
                        .set(VmInstanceVO_.lastHostUuid, lastHostUuid)
                        .update();

                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(final MigrateVolumeOnLocalStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("migrate-volume-%s-on-local-ps", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                migrateVolume(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
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

    private void migrateVolume(MigrateVolumeOnLocalStorageMsg msg, NoErrorCompletion completion) {
        MigrateVolumeOnLocalStorageReply reply = new MigrateVolumeOnLocalStorageReply();

        SimpleQuery<LocalStorageResourceRefVO> refq = dbf.createQuery(LocalStorageResourceRefVO.class);
        refq.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
        refq.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        LocalStorageResourceRefVO ref = refq.find();
        if (ref == null) {
            reply.setError(operr("volume[uuid:%s] is not on the local storage anymore," +
                    "it may have been deleted", msg.getVolumeUuid()));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        if (ref.getHostUuid().equals(msg.getDestHostUuid())) {
            logger.debug(String.format("the volume[uuid:%s] is already on the host[uuid:%s], no need to migrate",
                    msg.getVolumeUuid(), msg.getDestHostUuid()));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("migrate-volume-%s-local-storage-%s-to-host-%s",
                msg.getVolumeUuid(), msg.getPrimaryStorageUuid(), msg.getDestHostUuid()));
        chain.then(new ShareFlow() {
            LocalStorageResourceRefVO volumeRefVO;
            List<LocalStorageResourceRefVO> snapshotRefVOS;
            LocalStorageResourceRefInventory ref;
            long requiredSize;
            List<VolumeSnapshotVO> snapshots;
            VolumeVO volume;
            MigrateBitsStruct struct = new MigrateBitsStruct();
            LocalStorageHypervisorBackend bkd;
            VolumeStatus originVolumeStatus;

            {
                SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
                q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
                volumeRefVO = q.find();
                ref = LocalStorageResourceRefInventory.valueOf(volumeRefVO);

                SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                sq.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, ref.getResourceUuid());
                snapshots = sq.list();

                volume = dbf.findByUuid(ref.getResourceUuid(), VolumeVO.class);

                requiredSize = ratioMgr.calculateByRatio(self.getUuid(), ref.getSize());

                ResourceInfo info = new ResourceInfo();
                info.setResourceRef(ref);
                info.setPath(volume.getInstallPath());

                struct.getInfos().add(info);
                struct.setDestHostUuid(msg.getDestHostUuid());
                struct.setSrcHostUuid(ref.getHostUuid());
                struct.setVolume(VolumeInventory.valueOf(volume));

                if (!snapshots.isEmpty()) {
                    List<String> spUuids = transformAndRemoveNull(snapshots, ResourceVO::getUuid);

                    snapshotRefVOS = Q.New(LocalStorageResourceRefVO.class)
                            .eq(LocalStorageResourceRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                            .in(LocalStorageResourceRefVO_.resourceUuid, spUuids)
                            .list();

                    final Map<String, LocalStorageResourceRefVO> refMap =
                            toMap(snapshotRefVOS, LocalStorageResourceRefVO::getResourceUuid, Function.identity());

                    for (VolumeSnapshotVO vo : snapshots) {
                        LocalStorageResourceRefVO ref = refMap.get(vo.getUuid());
                        if (ref == null) {
                            throw new CloudRuntimeException(
                                    String.format("cannot find reference of snapshot[uuid:%s, name:%s] on the local storage[uuid:%s, name:%s]",
                                            vo.getUuid(), vo.getName(), self.getUuid(), self.getName()));
                        }

                        info = new ResourceInfo();
                        info.setPath(vo.getPrimaryStorageInstallPath());
                        info.setResourceRef(LocalStorageResourceRefInventory.valueOf(ref));
                        struct.getInfos().add(info);

                        requiredSize += vo.getSize();
                    }
                }

                LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getDestHostUuid());
                bkd = f.getHypervisorBackend(self);

                originVolumeStatus = volume.getStatus();
                volume.setStatus(VolumeStatus.Migrating);
                SQL.New(VolumeVO.class).set(VolumeVO_.status, VolumeStatus.Migrating).eq(VolumeVO_.uuid, volume.getUuid()).update();
            }

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-dest-host";
                    boolean success = false;

                    @Override
                    public boolean skip(Map data) {
                        return msg.isAllocatedPrimaryStorageCapacity();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getDestHostUuid(), requiredSize, self.getUuid());
                        success = true;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            returnStorageCapacityToHost(msg.getDestHostUuid(), requiredSize);
                        }
                        trigger.rollback();
                    }
                });

                List<Flow> flows = bkd.createMigrateBitsVolumeFlow(struct);
                for (Flow fl : flows) {
                    flow(fl);
                }

                flow(new Flow() {
                    String __name__ = "move-original-volume-to-trash";

                    private InstallPathRecycleInventory trashInv;

                    @Override
                    public boolean skip(Map data) {
                        return !msg.isKeepOriginalVolumeInTrash();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeVO volume = dbf.findByUuid(volumeRefVO.getResourceUuid(), VolumeVO.class);
                        volume.setInstallPath(getVolumeFolderPath(volume.getInstallPath()));
                        trashInv = trash.createTrash(TrashType.MigrateVolume, true, VolumeInventory.valueOf(volume));
                        logger.debug(String.format("trash volume[uuid:%s] source folder[path: %s] on host[%s]",
                                volume.getUuid(), volume.getInstallPath(), trashInv.getHostUuid()));
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (trashInv != null) {
                            trash.removeFromDb(trashInv.getTrashId());
                        }
                        trigger.rollback();
                    }

                    private String getVolumeFolderPath(String installPath) {
                        File vol = new File(installPath);
                        if (vol.getParentFile().getName().equals("snapshots")) {
                            return vol.getParentFile().getParent();
                        } else {
                            return vol.getParent();
                        }
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "change-reference-to-dst-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> resourceUuids = new ArrayList<>();
                        resourceUuids.add(volumeRefVO.getResourceUuid());
                        if (snapshotRefVOS != null) {
                            for (LocalStorageResourceRefVO r : snapshotRefVOS) {
                                resourceUuids.add(r.getResourceUuid());
                            }
                        }

                        UpdateQuery.New(LocalStorageResourceRefVO.class)
                                .set(LocalStorageResourceRefVO_.hostUuid, msg.getDestHostUuid())
                                .condAnd(LocalStorageResourceRefVO_.resourceUuid, Op.IN, resourceUuids)
                                .update();

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-src-host";

                    @Override
                    public boolean skip(Map data) {
                        return msg.isKeepOriginalVolumeInTrash();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnStorageCapacityToHost(ref.getHostUuid(), requiredSize);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-bits-on-the-src-host";

                    @Override
                    public boolean skip(Map data) {
                        return msg.isKeepOriginalVolumeInTrash();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> paths = new ArrayList<>();
                        paths.add(volume.getInstallPath());
                        for (VolumeSnapshotVO sp : snapshots) {
                            paths.add(sp.getPrimaryStorageInstallPath());
                        }

                        final Iterator<String> it = paths.iterator();
                        new Runnable() {
                            @Override
                            @AsyncThread
                            public void run() {
                                if (!it.hasNext()) {
                                    return;
                                }

                                final String path = it.next();
                                bkd.deleteBits(path, struct.getSrcHostUuid(), new Completion(trigger) {
                                    @Override
                                    public void success() {
                                        run();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO GC
                                        logger.warn(String.format("failed to delete %s on the host[uuid:%s], %s",
                                                path, struct.getSrcHostUuid(), errorCode));
                                        run();
                                    }
                                });
                            }
                        }.run();

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        new SQLBatch() {
                            //migrate the rooVolume and need to update the ClusterUuid of vm
                            @Override
                            protected void scripts() {
                                Boolean isRootVolume = (Q.New(VolumeVO.class).select(VolumeVO_.type)
                                        .eq(VolumeVO_.uuid, volumeRefVO.getResourceUuid())
                                        .findValue() == VolumeType.Root);
                                if (isRootVolume) {
                                    Tuple tuple = Q.New(VmInstanceVO.class)
                                            .select(VmInstanceVO_.clusterUuid, VmInstanceVO_.uuid)
                                            .eq(VmInstanceVO_.rootVolumeUuid, volumeRefVO.getResourceUuid()).findTuple();
                                    String originClusterUuid = tuple.get(0, String.class);
                                    String vmUuid = tuple.get(1, String.class);
                                    String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                                            .eq(HostVO_.uuid, msg.getDestHostUuid()).findValue();

                                    UpdateQuery updateQuery = sql(VmInstanceVO.class);
                                    updateQuery.eq(VmInstanceVO_.uuid, vmUuid);
                                    updateQuery.set(VmInstanceVO_.hostUuid, msg.getDestHostUuid());
                                    if (!originClusterUuid.equals(clusterUuid)) {
                                        updateQuery.set(VmInstanceVO_.clusterUuid, clusterUuid);
                                    }
                                    updateQuery.update();
                                }

                                sql(VolumeVO.class)
                                        .eq(VolumeVO_.uuid, volumeRefVO.getResourceUuid())
                                        .set(VolumeVO_.status, originVolumeStatus)
                                        .update();

                                LocalStorageResourceRefVO vo = Q.New(LocalStorageResourceRefVO.class)
                                        .eq(LocalStorageResourceRefVO_.resourceUuid, volumeRefVO.getResourceUuid())
                                        .eq(LocalStorageResourceRefVO_.primaryStorageUuid, volumeRefVO.getPrimaryStorageUuid())
                                        .eq(LocalStorageResourceRefVO_.hostUuid, msg.getDestHostUuid())
                                        .find();
                                reply.setInventory(LocalStorageResourceRefInventory.valueOf(vo));
                            }
                        }.execute();

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg, completion) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }
        }).start();
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof InitPrimaryStorageOnHostConnectedMsg) {
            handle((InitPrimaryStorageOnHostConnectedMsg) msg);
        } else if (msg instanceof RemoveHostFromLocalStorageMsg) {
            handle((RemoveHostFromLocalStorageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadImageToPrimaryStorageCacheMsg) {
            handle((DownloadImageToPrimaryStorageCacheMsg) msg);
        } else if (msg instanceof LocalStorageCreateEmptyVolumeMsg) {
            handle((LocalStorageCreateEmptyVolumeMsg) msg);
        } else if (msg instanceof LocalStorageDirectlyDeleteBitsMsg) {
            handle((LocalStorageDirectlyDeleteBitsMsg) msg);
        } else if (msg instanceof LocalStorageReserveHostCapacityMsg) {
            handle((LocalStorageReserveHostCapacityMsg) msg);
        } else if (msg instanceof LocalStorageReturnHostCapacityMsg) {
            handle((LocalStorageReturnHostCapacityMsg) msg);
        } else if (msg instanceof LocalStorageHypervisorSpecificMessage) {
            handle((LocalStorageHypervisorSpecificMessage) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsFromLocalStorageToBackupStorageMsg) {
            handle((UploadBitsFromLocalStorageToBackupStorageMsg) msg);
        } else if (msg instanceof GetVolumeRootImageUuidFromPrimaryStorageMsg) {
            handle((GetVolumeRootImageUuidFromPrimaryStorageMsg) msg);
        } else if (msg instanceof LocalStorageDeleteImageCacheOnPrimaryStorageMsg) {
            handle((LocalStorageDeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MigrateVolumeOnLocalStorageMsg) {
            handle((MigrateVolumeOnLocalStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if ((msg instanceof LocalStorageRecalculateCapacityMsg)) {
            handle((LocalStorageRecalculateCapacityMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if (msg instanceof GetPrimaryStorageUsageReportMsg) {
            handle((GetPrimaryStorageUsageReportMsg) msg);
        } else if (msg instanceof CommitVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CommitVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PullVolumeSnapshotOnPrimaryStorageMsg) {
            handle((PullVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CleanupAllVmMetadataOnPrimaryStorageMsg) {
            handle((CleanupAllVmMetadataOnPrimaryStorageMsg) msg);
        } else if (msg instanceof EncryptVolumeBitsOnPrimaryStorageMsg) {
            handle((EncryptVolumeBitsOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ConvertVolumeEncryptionOnPrimaryStorageMsg) {
            handle((ConvertVolumeEncryptionOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    protected void handle(ConvertVolumeEncryptionOnPrimaryStorageMsg msg) {
        ConvertVolumeEncryptionOnPrimaryStorageReply reply = new ConvertVolumeEncryptionOnPrimaryStorageReply();
        if (msg.getVolume() == null || StringUtils.isBlank(msg.getVolume().getUuid())) {
            reply.setError(operr("convert volume encryption on local primary storage[uuid:%s] requires volume with non-blank uuid",
                    self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        String hostUuid = StringUtils.isNotBlank(msg.getHostUuid()) ?
                msg.getHostUuid() : null;
        if (StringUtils.isBlank(hostUuid)) {
            try {
                hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
            } catch (OperationFailureException e) {
                reply.setError(e.getErrorCode());
                bus.reply(msg, reply);
                return;
            }
        }
        if (StringUtils.isBlank(hostUuid)) {
            reply.setError(operr("cannot determine host for converting volume[uuid:%s] encryption on local primary storage[uuid:%s]",
                    msg.getVolume().getUuid(), self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ConvertVolumeEncryptionOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(EncryptVolumeBitsOnPrimaryStorageMsg msg) {
        if (StringUtils.isBlank(msg.getHostUuid()) || StringUtils.isBlank(msg.getVolumeUuid())) {
            EncryptVolumeBitsOnPrimaryStorageReply reply = new EncryptVolumeBitsOnPrimaryStorageReply();
            reply.setError(operr("encrypt volume bits on local primary storage[uuid:%s] requires non-blank hostUuid and volumeUuid",
                    self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(EncryptVolumeBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                EncryptVolumeBitsOnPrimaryStorageReply reply = new EncryptVolumeBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getSrcHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadBitsFromKVMHostToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetDownloadBitsFromKVMHostProgressMsg msg) {
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply>(msg) {

            @Override
            public void success(GetDownloadBitsFromKVMHostProgressReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getDestHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
            APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
            imageCacheCleaner.setForce(msg.isForce());
            imageCacheCleaner.cleanup(msg.getUuid(), false);
            bus.publish(evt);
    }


    private void handle(final LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteImageCacheOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeRootImageUuidFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        String hostUuid;
        if (msg.getHostUuid() == null) {
            hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
            msg.setHostUuid(hostUuid);
        }
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeBackingChainFromPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeBackingChainFromPrimaryStorageReply reply = new GetVolumeBackingChainFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetPrimaryStorageUsageReportMsg msg) {
        GetPrimaryStorageUsedPhysicalCapacityForecastReply reply = new GetPrimaryStorageUsedPhysicalCapacityForecastReply();
        reply.setUsageReportMap(localForecaster.getUsageReportByResourceUuids(getHostUuidsFromPoolUris(msg.getUris())));
        bus.reply(msg, reply);
    }

    private List<String> getHostUuidsFromPoolUris(List<String> uris) {
        return uris.stream().map(uri -> uri.replace("hostUuid://", "")).collect(Collectors.toList());
    }

    private void handle(final UploadBitsFromLocalStorageToBackupStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsFromLocalStorageToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsFromLocalStorageToBackupStorageReply reply = new UploadBitsFromLocalStorageToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
            @Override
            public void success(CreateTemporaryVolumeFromSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(LocalStorageHypervisorSpecificMessage msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handleHypervisorSpecificMessage(msg);
    }

    private void handle(LocalStorageReturnHostCapacityMsg msg) {
        LocalStorageReturnHostCapacityReply reply = new LocalStorageReturnHostCapacityReply();
        long size = msg.isNoOverProvisioning() ? msg.getSize() : ratioMgr.calculateByRatio(self.getUuid(), msg.getSize());
        returnStorageCapacityToHost(msg.getHostUuid(), size);
        bus.reply(msg, reply);
    }

    private void handle(LocalStorageReserveHostCapacityMsg msg) {
        LocalStorageReserveHostCapacityReply reply = new LocalStorageReserveHostCapacityReply();
        long size = msg.isNoOverProvisioning() ? msg.getSize() : ratioMgr.calculateByRatio(self.getUuid(), msg.getSize());
        reserveCapacityOnHost(msg.getHostUuid(), size, self.getUuid());
        bus.reply(msg, reply);
    }

    private void handle(final LocalStorageDirectlyDeleteBitsMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply>(msg) {
            @Override
            public void success(LocalStorageDirectlyDeleteBitsReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                LocalStorageDirectlyDeleteBitsReply reply = new LocalStorageDirectlyDeleteBitsReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final LocalStorageCreateEmptyVolumeMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply>(msg) {
            @Override
            public void success(LocalStorageCreateEmptyVolumeReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                LocalStorageCreateEmptyVolumeReply reply = new LocalStorageCreateEmptyVolumeReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetLocalStorageHostDiskCapacityMsg msg) {
        APIGetLocalStorageHostDiskCapacityReply reply = new APIGetLocalStorageHostDiskCapacityReply();
        if (msg.getHostUuid() != null) {
            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
            q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, msg.getHostUuid());
            LocalStorageHostRefVO ref = q.find();

            long total = 0;
            long available = 0;
            long availablePhy = 0;
            long totalPhy = 0;

            if (ref == null) {
                HostStatus status = Q.New(HostVO.class).select(HostVO_.status)
                        .eq(HostVO_.uuid, msg.getHostUuid()).findValue();
                if (status == HostStatus.Connected) {
                    reply.setError(err(SysErrors.RESOURCE_NOT_FOUND,
                            "local primary storage[uuid:%s] doesn't have the host[uuid:%s]",
                            self.getUuid(), msg.getHostUuid()));
                    bus.reply(msg, reply);
                    return;
                }
            } else {
                total = ref.getTotalCapacity();
                available = ref.getAvailableCapacity();
                availablePhy = ref.getAvailablePhysicalCapacity();
                totalPhy = ref.getTotalPhysicalCapacity();
            }

            HostDiskCapacity c = new HostDiskCapacity();
            c.setHostUuid(msg.getHostUuid());
            c.setTotalCapacity(total);
            c.setAvailableCapacity(available);
            c.setAvailablePhysicalCapacity(availablePhy);
            c.setTotalPhysicalCapacity(totalPhy);
            reply.setInventories(list(c));
        } else {
            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
            List<LocalStorageHostRefVO> refs = q.list();

            List<HostDiskCapacity> cs = transformAndRemoveNull(refs, ref -> {
                HostDiskCapacity c = new HostDiskCapacity();
                c.setHostUuid(ref.getHostUuid());
                c.setTotalCapacity(ref.getTotalCapacity());
                c.setAvailableCapacity(ref.getAvailableCapacity());
                c.setAvailablePhysicalCapacity(ref.getAvailablePhysicalCapacity());
                c.setTotalPhysicalCapacity(ref.getTotalPhysicalCapacity());
                return c;
            });

            reply.setInventories(cs);
        }

        bus.reply(msg, reply);
    }

    private void handle(final DownloadImageToPrimaryStorageCacheMsg msg) {
        ImageInventory imageInventory = msg.getImage();
        // If image actualSize is null, Default allow distribute image
        long imageActualSize = imageInventory.getActualSize() != null ? imageInventory.getActualSize() : 0;
        final DownloadImageToPrimaryStorageCacheReply reply = new DownloadImageToPrimaryStorageCacheReply();
        final List<String> hostUuids;
        if (msg.getHostUuid() == null) {
            hostUuids = new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select h.hostUuid" +
                            " from LocalStorageHostRefVO h, HostVO host" +
                            " where h.primaryStorageUuid = :puuid" +
                            " and h.hostUuid = host.uuid" +
                            " and host.status = :hstatus" +
                            " and h.availablePhysicalCapacity >= :availablePhysicalCapacity";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("puuid", self.getUuid());
                    q.setParameter("hstatus", HostStatus.Connected);
                    q.setParameter("availablePhysicalCapacity", imageActualSize);
                    return q.getResultList();
                }
            }.call();
        } else {
            hostUuids = list(msg.getHostUuid());
        }

        if (hostUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        class HostError {
            ErrorCode errorCode;
            String hostUuid;
        }

        class Ret {
            List<HostError> errorCodes = new ArrayList<HostError>();
            String installPath;

            synchronized void addError(HostError err) {
                errorCodes.add(err);
            }
        }

        final Ret ret = new Ret();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                if (ret.errorCodes.size() == hostUuids.size()) {
                    reply.setError(operr("failed to download image[uuid:%s] to all hosts in the local storage[uuid:%s]" +
                            ". %s", msg.getImage().getUuid(), self.getUuid(), JSONObjectUtil.toJsonString(ret.errorCodes)));
                } else if (!ret.errorCodes.isEmpty()) {
                    for (HostError err : ret.errorCodes) {
                        logger.warn(String.format("failed to download image [uuid:%s] to the host[uuid:%s] in the local" +
                                " storage[uuid:%s]. %s", msg.getImage().getUuid(), err.hostUuid, self.getUuid(), err.errorCode));
                    }
                }

                reply.setInstallPath(ret.installPath);
                bus.reply(msg, reply);
            }
        });

        for (final String hostUuid : hostUuids) {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("download-image-%s-to-local-storage-%s-host-%s", msg.getImage().getUuid(), self.getUuid(), hostUuid));
            chain.then(new ShareFlow() {
                @Override
                public void setup() {

                    flow(new NoRollbackFlow() {
                        String __name__ = "download-to-host";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                            LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                            bkd.downloadImageToCache(msg.getImage(), hostUuid, new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String returnValue) {
                                    ret.installPath = returnValue;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(latch) {
                        @Override
                        public void handle(Map data) {
                            latch.ack();
                        }
                    });

                    error(new FlowErrorHandler(latch) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            HostError herr = new HostError();
                            herr.errorCode = errCode;
                            herr.hostUuid = hostUuid;
                            ret.addError(herr);
                            latch.ack();
                        }
                    });
                }
            }).start();
        }
    }

    @Override
    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getTo().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        MergeVolumeSnapshotOnPrimaryStorageReply r = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bkd.stream(msg.getFrom(), msg.getTo(), msg.isFullRebase(), hostUuid, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);

        FlattenVolumeOnPrimaryStorageReply r = new FlattenVolumeOnPrimaryStorageReply();
        bkd.stream(null, msg.getVolume(), true, hostUuid, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final VolumeSnapshotInventory sinv = msg.getSnapshot();
        final String hostUuid = getHostUuidByResourceUuid(sinv.getUuid());
        if (hostUuid == null) {
            throw new OperationFailureException(inerr(
                    "the volume snapshot[uuid:%s] is not on the local primary storage[uuid: %s]; the local primary storage" +
                            " doesn't support the manner of downloading snapshots and creating the volume", sinv.getUuid(), self.getUuid()
            ));
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-volume-%s-from-snapshots", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolumeUuid(), VolumeVO.class.getSimpleName(), reply.getSize(), hostUuid);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply>(msg) {
            @Override
            public void success(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                long increment = returnValue.getSize() - msg.getVolume().getSize();
                long size = ratioMgr.calculateByRatio(self.getUuid(), increment);
                if (size > 0) {
                    reserveCapacityOnHost(hostUuid, size, msg.getPrimaryStorageUuid());
                } else if (size < 0) {
                    returnStorageCapacityToHost(hostUuid, Math.abs(size));
                }

                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ReInitRootVolumeFromTemplateOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected String getHostUuidByResourceUuid(String resUuid) {
        String huuid;
        huuid = new SQLBatchWithReturn<String>() {
            private String findHostByUuid(String uuid) {
                return sql("select uuid from HostVO where uuid = :uuid", String.class).param("uuid", uuid).find();
            }

            @Override
            protected String scripts() {
                String uuid = sql("select hostUuid from LocalStorageResourceRefVO where resourceUuid = :resUuid", String.class)
                        .param("resUuid", resUuid)
                        .find();
                if (uuid == null) {
                    throw new OperationFailureException(operr("cannot find any host which has resource[uuid:%s]", resUuid));
                } else if (findHostByUuid(uuid) == null) {
                    throw new OperationFailureException(
                            operr("Resource[uuid:%s] can only be operated on host[uuid:%s], but the host has been deleted",
                                    resUuid, uuid));
                }
                return uuid;
            }
        }.execute();
        return huuid;
    }

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-snapshot-%s-on-local-storage-%s", msg.getSnapshot().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteSnapshotOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-snapshot-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, hostUuid, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(trigger) {
                            @Override
                            public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnStorageCapacityToHost(hostUuid, msg.getSnapshot().getSize());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getSnapshot().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(CheckSnapshotMsg msg) {
        CheckSnapshotReply reply = new CheckSnapshotReply();
        final String hostUuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final TakeSnapshotMsg msg) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        final String hostUuid = getHostUuidByResourceUuid(sp.getVolumeUuid());

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<TakeSnapshotReply>(msg) {
            @Override
            public void success(TakeSnapshotReply returnValue) {
                createResourceRefVO(sp.getUuid(), VolumeSnapshotVO.class.getSimpleName(), returnValue.getInventory().getSize(), hostUuid);
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                TakeSnapshotReply reply = new TakeSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CommitVolumeSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CommitVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CommitVolumeSnapshotOnPrimaryStorageReply reply = new CommitVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final PullVolumeSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(PullVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(RebaseVolumeBackingFileOnPrimaryStorageMsg msg) {
        final LocalStorageHypervisorBackend bkd;
        try {
            LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
            bkd = f.getHypervisorBackend(self);
        } catch (Exception e) {
            RebaseVolumeBackingFileOnPrimaryStorageReply reply = new RebaseVolumeBackingFileOnPrimaryStorageReply();
            reply.setError(operr("failed to resolve host/backend for rebase on local primary storage[uuid:%s], hostUuid[%s]: %s",
                    self.getUuid(), msg.getHostUuid(), e.getMessage()));
            bus.reply(msg, reply);
            return;
        }
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<RebaseVolumeBackingFileOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RebaseVolumeBackingFileOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RebaseVolumeBackingFileOnPrimaryStorageReply reply = new RebaseVolumeBackingFileOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(RemoveHostFromLocalStorageMsg msg) {
        RemoveHostFromLocalStorageReply reply = new RemoveHostFromLocalStorageReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("remove-host-%s-from-localStorage-%s", msg.getHostUuid(), msg.getPrimaryStorageUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                removeHostFromLocalStorage(msg, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
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

    private void removeHostFromLocalStorage(RemoveHostFromLocalStorageMsg msg, Completion completion) {
        LocalStorageHostRefVO ref = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, msg.getHostUuid())
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .find();

        List<VolumeVO> volumeVOS = SQL.New("select vo from VolumeVO vo, LocalStorageResourceRefVO ref " +
                "where vo.uuid = ref.resourceUuid and ref.hostUuid =:hostUuid " +
                "and ref.primaryStorageUuid=:primaryStorageUuid and ref.resourceType=:resourceType " +
                "and vo.type=:type")
                .param("hostUuid", msg.getHostUuid())
                .param("primaryStorageUuid", self.getUuid())
                .param("resourceType", VolumeVO.class.getSimpleName())
                .param("type", VolumeType.Root)
                .list();


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("remove-host-from-localStorage");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "remove-volume-under-resource";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        //ZSTAC-34201 delete resources under volumes
                        for (VolumeVO vo : volumeVOS) {
                            pluginRgty.getExtensionList(VolumeJustBeforeDeleteFromDbExtensionPoint.class).forEach(ext-> ext.volumeJustBeforeDeleteFromDb(VolumeInventory.valueOf(vo)));
                        }

                        trigger.next();

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "remove-vm-under-resource";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> volumeUuids = volumeVOS.stream().map(VolumeVO::getUuid).collect(Collectors.toList());
                        if (!volumeUuids.isEmpty()) {
                            List<VmInstanceVO> vmInstanceVOS = SQL.New("select vm from VmInstanceVO vm where vm.rootVolumeUuid in" +
                                    " (select vol.uuid from VolumeVO vol where vol.uuid in (:volUuids)" +
                                    " and vol.type = :volType)")
                                    .param("volUuids", volumeUuids)
                                    .param("volType", VolumeType.Root).list();

                            for (VmInstanceVO vo : vmInstanceVOS) {
                                pluginRgty.getExtensionList(VmJustBeforeDeleteFromDbExtensionPoint.class).forEach(ext-> ext.vmJustBeforeDeleteFromDb(VmInstanceInventory.valueOf(vo)));
                            }

                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String  __name__ = "delete-host-resource";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        //ZSTAC-9635
                        deleteResourceRef(msg.getHostUuid());
                        if (ref != null) {
                            dbf.remove(ref);
                            decreaseCapacity(ref.getTotalCapacity(),
                                    ref.getAvailableCapacity(),
                                    ref.getTotalPhysicalCapacity(),
                                    ref.getAvailablePhysicalCapacity(),
                                    ref.getSystemUsedCapacity());
                        }

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

    void deleteResourceRef(String hostUuid) {
        new SQLBatch() {
            // dirty cleanup database for all possible related entities linking to the local storage.
            // basically, we cleanup, volumes, volume snapshots, image caches.
            // all below sql must be executed as the order they defined, DO NOT change anything unless you know
            // exactly what you are doing.
            // the MySQL won't support cascade trigger, which means when you delete a VM its nic will be
            // deleted accordingly but the trigger installed on the `AFTER DELETE ON VmNicVO` will not
            // be executed.
            // so we have to explicitly delete VmNicVO, VolumeVO then VmInstanceVO in order, to make
            // mysql triggers work in order to delete entities in AccountResourceRefVO, SystemVO etc.

            @Override
            protected void scripts() {

                // delete the image cache
                sql("delete from ImageCacheVO ic where ic.primaryStorageUuid = :psUuid and" +
                        " ic.installUrl like :url").param("psUuid", self.getUuid())
                        .param("url", String.format("%%%s%%", hostUuid)).execute();

                List<LocalStorageResourceRefVO> refs = sql(
                        "select ref from LocalStorageResourceRefVO ref where ref.hostUuid = :huuid" +
                                " and ref.primaryStorageUuid = :psUuid", LocalStorageResourceRefVO.class
                ).param("huuid", hostUuid).param("psUuid", self.getUuid()).list();

                if (refs.isEmpty()) {
                    return;
                }

                List<String> volumesUuids = new ArrayList<>();
                List<String> snapshotUuids = new ArrayList<>();
                List<String> snapshotTreeUuids = new ArrayList<>();
                for (LocalStorageResourceRefVO ref : refs) {
                    if (VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
                        volumesUuids.add(ref.getResourceUuid());
                    } else if (VolumeSnapshotVO.class.getSimpleName().equals(ref.getResourceType())) {
                        snapshotUuids.add(ref.getResourceUuid());
                    }
                }

                if (!snapshotUuids.isEmpty()) {
                    List<String> treeList = sql(
                            "select treeUuid from VolumeSnapshotVO where uuid in (:uuids) group by treeUuid", String.class)
                            .param("uuids", snapshotUuids).list();
                    if (treeList != null) {
                        snapshotTreeUuids.addAll(treeList);
                    }

                    sql("delete from VolumeSnapshotVO sp where sp.uuid in (:uuids)")
                            .param("uuids", snapshotUuids).execute();

                    logger.debug(String.format("delete volume snapshots%s because the host[uuid:%s] is removed from" +
                            " the local storage[name:%s, uuid:%s]", snapshotUuids, hostUuid, self.getName(), self.getUuid()));
                }

                if (!snapshotTreeUuids.isEmpty()) {
                    for (String snapshotTreeUuid : snapshotTreeUuids) {
                        if (q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.treeUuid, snapshotTreeUuid).isExists()) {
                            break;
                        }
                        logger.debug(String.format("volume snapshot tree[uuid:%s] has no leaf, delete it", snapshotTreeUuid));
                        sql(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, snapshotTreeUuid).hardDelete();
                    }
                }

                if (!volumesUuids.isEmpty()) {
                    List<String> vmUuidsToDelete = sql("select vm.uuid from VmInstanceVO vm where vm.rootVolumeUuid in" +
                            " (select vol.uuid from VolumeVO vol where vol.uuid in (:volUuids)" +
                            " and vol.type = :volType)", String.class)
                            .param("volUuids", volumesUuids).param("volType", VolumeType.Root).list();


                    if (!vmUuidsToDelete.isEmpty()) {
                        // delete vm nics
                        sql("delete from VmNicVO nic where nic.vmInstanceUuid in (:uuids)")
                                .param("uuids", vmUuidsToDelete).execute();
                    }

                    if (!vmUuidsToDelete.isEmpty()) {
                        // delete vm cdrom
                        sql("delete from VmCdRomVO cdrom where cdrom.vmInstanceUuid in (:uuids)")
                                .param("uuids", vmUuidsToDelete).execute();
                    }
                    // delete volumes including root and data volumes
                    sql("delete from VolumeVO vol where vol.uuid in (:uuids)")
                            .param("uuids", volumesUuids).execute();
                    logger.debug(String.format("delete volumes%s because the host[uuid:%s] is removed from" +
                            " the local storage[name:%s, uuid:%s]", volumesUuids, hostUuid, self.getName(), self.getUuid()));

                    if (!vmUuidsToDelete.isEmpty()) {
                        // delete the vms
                        sql("delete from VmInstanceVO vm where vm.uuid in (:uuids)")
                                .param("uuids", vmUuidsToDelete).execute();

                        logger.debug(String.format("delete VMs%s because the host[uuid:%s] is removed from" +
                                " the local storage[name:%s, uuid:%s]", vmUuidsToDelete, hostUuid, self.getName(), self.getUuid()));
                    }
                }

                for (LocalStorageResourceRefVO ref : refs) {
                    dbf.getEntityManager().merge(ref);
                    dbf.getEntityManager().remove(ref);
                }
            }
        }.execute();
    }

    private void handle(LocalStorageRecalculateCapacityMsg msg) {
        recalculateLocalStorageCapacity(msg.isNeedRecalculateRef(), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                LocalStorageRecalculateCapacityReply reply = new LocalStorageRecalculateCapacityReply();
                bus.reply(msg, reply);
            }
        });
    }

    private void recalculateLocalStorageCapacity(boolean needRecalculateRef, NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("recalculate-local-storage-capacity-%s", self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                LocalStorageCapacityRecalculator c = new LocalStorageCapacityRecalculator();
                if (needRecalculateRef) {
                    c.calculateByPrimaryStorageUuid(self.getUuid());
                }
                c.calculateTotalCapacity(self.getUuid());
                completion.done();
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            protected int getMaxPendingTasks() {
                return 0;
            }

            protected void exceedMaxPendingCallback() {
                completion.done();
            }

            // separate calculate all refs and calculate total to different sub queue
            protected String getDeduplicateString() {
                return needRecalculateRef ? "calculate-all-refs" : "calculate-total";
            }
        });
    }

    @Override
    protected void handle(RecalculatePrimaryStorageCapacityMsg msg) {
        recalculateLocalStorageCapacity(true, new NoErrorCompletion(msg) {
            @Override
            public void done() {
                RecalculatePrimaryStorageCapacityReply reply = new RecalculatePrimaryStorageCapacityReply();
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final InitPrimaryStorageOnHostConnectedMsg msg) {
        final InitPrimaryStorageOnHostConnectedReply reply = new InitPrimaryStorageOnHostConnectedReply();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("init primarystorage on host connected");
        chain.then(new NoRollbackFlow() {
            String __name__ = "initial db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid(), false);
                final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);

                bkd.handle(msg, new ReturnValueCompletion<PhysicalCapacityUsage>(msg) {
                    @Override
                    public void success(PhysicalCapacityUsage usage) {
                        LocalStoragePhysicalCapacityUsage c = (LocalStoragePhysicalCapacityUsage)usage;

                        List<LocalStorageHostRefVO> refs = Q.New(LocalStorageHostRefVO.class)
                                .eq(LocalStorageHostRefVO_.hostUuid, msg.getHostUuid())
                                .eq(LocalStorageHostRefVO_.primaryStorageUuid, self.getUuid())
                                .list();

                        LocalStorageHostRefVO ref;
                        if (refs == null || refs.isEmpty()) {
                            ref = new LocalStorageHostRefVO();
                            ref.setTotalCapacity(c.totalPhysicalSize);
                            ref.setAvailableCapacity(c.availablePhysicalSize);
                            ref.setTotalPhysicalCapacity(c.totalPhysicalSize);
                            ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                            ref.setHostUuid(msg.getHostUuid());
                            ref.setPrimaryStorageUuid(self.getUuid());
                            ref.setSystemUsedCapacity(c.totalPhysicalSize - c.availablePhysicalSize - c.localStorageUsedSize);
                            dbf.persist(ref);

                            if (msg.isNewAddedHost()) {
                                increaseCapacity(
                                        c.totalPhysicalSize,
                                        c.availablePhysicalSize,
                                        c.totalPhysicalSize,
                                        c.availablePhysicalSize,
                                        ref.getSystemUsedCapacity());
                            } else {
                                // this happened when manually delete LocalStorageHostRefVO
                                LocalStorageRecalculateCapacityMsg rmsg = new LocalStorageRecalculateCapacityMsg();
                                rmsg.setPrimaryStorageUuid(self.getUuid());
                                rmsg.setNeedRecalculateRef(true);
                                bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                                bus.send(rmsg);
                            }
                        } else {
                            ref = refs.get(0);
                            long originSystemUsed = ref.getSystemUsedCapacity();

                            ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                            ref.setTotalPhysicalCapacity(c.totalPhysicalSize);
                            ref.setTotalCapacity(c.totalPhysicalSize);
                            ref.setSystemUsedCapacity(c.totalPhysicalSize - c.availablePhysicalSize - c.localStorageUsedSize);
                            dbf.update(ref);

                            if (originSystemUsed != ref.getSystemUsedCapacity()) {
                                increaseCapacity(null, null, null, null, ref.getSystemUsedCapacity() - originSystemUsed);
                            }

                            // the host's local storage capacity changed
                            // need to recalculate the capacity in the database
                            LocalStorageRecalculateCapacityMsg rmsg = new LocalStorageRecalculateCapacityMsg();
                            rmsg.setPrimaryStorageUuid(self.getUuid());
                            rmsg.setNeedRecalculateRef(false);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                            bus.send(rmsg);
                        }

                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create initailized file";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostInventory host = HostInventory.valueOf(dbf.findByUuid(msg.getHostUuid(), HostVO.class));
                checkLocalStoragePrimaryStorageInitilized(CollectionDSL.list(host), true, new Completion(trigger) {
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
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    /**
     * reserve capacity on Host
     * @deprecated
     * <p> the AllocatePrimaryStorageSpaceMsg will reserve capacity on localPrimaryStorage and host in one transaction.
     * <p> reference {@link AllocatePrimaryStorageSpaceMsg}
     *
     * For example, to create volume snapshot
     * <p> reference {@link AllocatePrimaryStorageSpaceMsg#setForce(boolean)} ()}
     * <p> boolean = true
     *
     * <p> For example, to reserve storage capacity to localPrimaryStorage and host
     * <p> {@link AllocatePrimaryStorageSpaceMsg#setRequiredInstallUri(String)}}
     * <p> String = file://$URL;hostUuid://$HOSTUUID
     */
    @Deprecated
    @ExceptionSafe
    protected void reserveCapaciryOnHostIgnoreError(String hostUuid, long size, String psUuid) {
        new LocalStorageUtils().reserveCapacityOnHost(hostUuid, size, psUuid, self, true);
    }

    /**
     * reserve capacity on Host
     * @deprecated
     * <p> the AllocatePrimaryStorageSpaceMsg will reserve capacity on localPrimaryStorage and host in one transaction.
     * <p> reference {@link AllocatePrimaryStorageSpaceMsg}
     *
     * <p> For example, to reserve storage capacity to localPrimaryStorage and host
     * <p> {@link AllocatePrimaryStorageSpaceMsg#setRequiredInstallUri(String)}}
     * <p> String = file://$URL;hostUuid://$HOSTUUID
     */
    @Deprecated
    protected void reserveCapacityOnHost(String hostUuid, long size, String psUuid) {
        new LocalStorageUtils().reserveCapacityOnHost(hostUuid, size, psUuid, self, false);
    }

    /**
     * return capacity on Host
     * @deprecated
     * <p> the ReleasePrimaryStorageSpaceMsg will return capacity on localPrimaryStorage and host in one transaction.
     * <p> reference {@link ReleasePrimaryStorageSpaceMsg}
     *
     * <p> For example, to return storage capacity to localPrimaryStorage and host
     * <p>{@link ReleasePrimaryStorageSpaceMsg#setAllocatedInstallUrl(String)}
     * <p>String = file://$URL;hostUuid://$HOSTUUID
     */
    @Deprecated
    @Transactional
    protected void returnStorageCapacityToHost(String hostUuid, long size) {
        new LocalStorageUtils().returnStorageCapacityToHost(hostUuid, size, self);
    }

    /**
     * return capacity on Host
     * @deprecated
     * <p> the ReleasePrimaryStorageSpaceMsg will return capacity on localPrimaryStorage and host in one transaction.
     * <p> reference {@link ReleasePrimaryStorageSpaceMsg}
     *
     * <p> For example, to return storage capacity to localPrimaryStorage and host
     * <p> {@link ReleasePrimaryStorageSpaceMsg#setAllocatedInstallUrl(String)}
     * <p> String = volume://$VOLUMEUUID
     */
    @Deprecated
    @Transactional
    protected void returnStorageCapacityToHostByResourceUuid(String resUuid) {
        String sql = "select href, rref" +
                " from LocalStorageHostRefVO href, LocalStorageResourceRefVO rref" +
                " where href.hostUuid = rref.hostUuid" +
                " and href.primaryStorageUuid = rref.primaryStorageUuid" +
                " and rref.resourceUuid = :resUuid" +
                " and rref.primaryStorageUuid = :puuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        List<Tuple> tupleList = q.getResultList();
        if (tupleList == null || tupleList.isEmpty()) {
            return;
        }

        DebugUtils.Assert(tupleList.size() == 1,
                "should not get more than one LocalStorageHostRefVO/LocalStorageResourceRefVO");
        Tuple twoRefs = tupleList.get(0);
        LocalStorageHostRefVO href = twoRefs.get(0, LocalStorageHostRefVO.class);
        LocalStorageResourceRefVO rref = twoRefs.get(1, LocalStorageResourceRefVO.class);

        long requiredSize = rref.getSize();
        if (VolumeVO.class.getSimpleName().equals(rref.getResourceType())) {
            requiredSize = ratioMgr.calculateByRatio(self.getUuid(), requiredSize);
        }

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setSizeBeforeOverProvisioning(rref.getSize());
        s.setHostUuid(href.getHostUuid());
        s.setLocalStorage(getSelfInventory());
        s.setSize(requiredSize);
        for (LocalStorageReturnHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReturnHostCapacityExtensionPoint.class)) {
            ext.beforeReturnLocalStorageCapacityOnHost(s);
        }

        LocalStorageUtils.logCapacityChange(self.getUuid(), href.getHostUuid(), href.getAvailableCapacity(), href.getAvailableCapacity() + s.getSize());
        href.setAvailableCapacity(href.getAvailableCapacity() + s.getSize());
        dbf.getEntityManager().merge(href);
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        String hostUuid = msg.getDestHost().getUuid();
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        final long size = msg.getVolume().getSize();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        chain.enableProgressReport();

        final String finalHostUuid = hostUuid;
        chain.then(new ShareFlow() {
            InstantiateVolumeOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "instantiate-volume-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(InstantiateVolumeOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName(),
                                msg.getVolume().getSize() == 0 ? size : msg.getVolume().getSize(), finalHostUuid);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void deleteResourceRefVO(String resourceUuid) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
        LocalStorageResourceRefVO ref = q.find();
        if (ref != null) {
            dbf.remove(ref);
        }
    }

    private void createResourceRefVO(String resUuid, String resType, long size, String hostUuid) {
        LocalStorageResourceRefVO ref = new LocalStorageResourceRefVO();
        ref.setPrimaryStorageUuid(self.getUuid());
        ref.setSize(size);
        ref.setResourceType(resType);
        ref.setResourceUuid(resUuid);
        ref.setHostUuid(hostUuid);
        dbf.persist(ref);
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        String sql = "select host" +
                " from LocalStorageResourceRefVO h, HostVO host" +
                " where h.hostUuid = host.uuid" +
                " and h.resourceUuid = :resourceUuid" +
                " and h.resourceType = :resourceType";

        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("resourceUuid", msg.getVolume().getUuid());
        q.setParameter("resourceType", VolumeVO.class.getSimpleName());

        if (q.getResultList().isEmpty()) {
            logger.debug(String.format("volume[uuid:%s] is not on the local storage[uuid:%s, name:%s]," +
                            "the host the volume is on may have been deleted",
                    msg.getVolume().getUuid(), self.getUuid(), self.getName()));

            DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
            bus.reply(msg, reply);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteVolumeOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteVolumeOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnStorageCapacityToHostByResourceUuid(msg.getVolume().getUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getVolume().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeInventory().getUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeOnPrimaryStorageReply r = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeSnapshot().getUuid(), VolumeSnapshotVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply r = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeInventory().getUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeOnPrimaryStorageReply r = new CreateTemplateFromVolumeOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        if (msg.getHostUuid() == null && msg.getAllocatedInstallUrl() == null) {
            throw new OperationFailureException(operr("unable to create the data volume[uuid: %s] on a local primary storage[uuid:%s], because the hostUuid is not specified.",
                    msg.getVolumeUuid(), self.getUuid()));
        }

        if (msg.getAllocatedInstallUrl() != null) {
            msg.setHostUuid(getHostUuidFromInstallUrl(msg.getAllocatedInstallUrl()));
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-data-volume-%s-to-local-storage-%s", msg.getVolumeUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DownloadDataVolumeToPrimaryStorageReply reply;

            long requiredSize = ratioMgr.calculateByRatio(self.getUuid(), msg.getImage().getSize());

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-the-data-volume-to-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(trigger) {
                            @Override
                            public void success(DownloadDataVolumeToPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolumeUuid(), VolumeVO.class.getSimpleName(), msg.getImage().getSize(), msg.getHostUuid());
                        saveVolumeProvisioningStrategy(msg.getVolumeUuid(), VolumeProvisioningStrategy.ThinProvisioning);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        if (msg.getHostUuid() == null && msg.getAllocatedInstallUrl() == null) {
            throw new OperationFailureException(operr("unable to create the data volume[uuid: %s] on a local primary storage[uuid:%s], because the hostUuid is not specified.",
                    msg.getVolumeUuid(), self.getUuid()));
        }

        if (msg.getAllocatedInstallUrl() != null) {
            msg.setHostUuid(getHostUuidFromInstallUrl(msg.getAllocatedInstallUrl()));
        }

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply>(msg) {
            @Override
            public void success(GetInstallPathForDataVolumeDownloadReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-bits-on-local-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteVolumeBitsOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-bits-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = msg.getHypervisorType() != null ? getHypervisorBackendFactory(msg.getHypervisorType()) :
                                getHypervisorBackendFactoryByResourceUuid(msg.getBitsUuid(), msg.getBitsType());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteVolumeBitsOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        if (!msg.isFromRecycle()) {
                            deleteResourceRefVO(msg.getBitsUuid());
                        }
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply>(msg) {
            DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();

            @Override
            public void success(DownloadVolumeTemplateToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-bits-on-local-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteBitsOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-bits-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteBitsOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-iso-%s-local-primary-storage-%s", msg.getIsoSpec().getInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DownloadIsoToPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-iso-to-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getDestHostUuid());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DownloadIsoToPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();

                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        boolean isExists = Q.New(LocalStorageResourceRefVO.class)
                                .eq(LocalStorageResourceRefVO_.resourceUuid, msg.getIsoSpec().getInventory().getUuid())
                                .eq(LocalStorageResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                                .eq(LocalStorageResourceRefVO_.primaryStorageUuid, self.getUuid())
                                .eq(LocalStorageResourceRefVO_.hostUuid, msg.getDestHostUuid())
                                .isExists();
                        if (!isExists) {
                            createResourceRefVO(
                                    msg.getIsoSpec().getInventory().getUuid(),
                                    ImageVO.class.getSimpleName(),
                                    msg.getIsoSpec().getInventory().getActualSize(),
                                    msg.getDestHostUuid()
                            );
                        }

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-iso-local-primary-storage-%s", msg.getIsoSpec().getInventory().getUuid()));
        chain.then(new ShareFlow() {
            DeleteIsoFromPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-iso-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(
                                msg.getIsoSpec().getInventory().getUuid(), ImageVO.class.getSimpleName());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteIsoFromPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);

                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnStorageCapacityToHostByResourceUuid(msg.getIsoSpec().getInventory().getUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getIsoSpec().getInventory().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setSupport(true);

        String volumeType = msg.getVolume().getType();
        if (VolumeType.Data.toString().equals(volumeType) || VolumeType.Root.toString().equals(volumeType)) {
            capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
        } else if (VolumeType.Memory.toString().equals(volumeType)) {
            capability.setArrangementType(VolumeSnapshotArrangementType.INDIVIDUAL);
        } else {
            throw new CloudRuntimeException(String.format("unknown volume type %s", volumeType));
        }

        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        String huuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        bkd.handle(msg, huuid, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(SyncVolumeSizeOnPrimaryStorageReply returnValue) {
                saveVolumeProvisioningStrategy(msg.getVolumeUuid(), returnValue.getActualSize() < returnValue.getSize() ? VolumeProvisioningStrategy.ThinProvisioning : VolumeProvisioningStrategy.ThickProvisioning);
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        String huuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        bkd.handle(msg, huuid, new ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(EstimateVolumeTemplateSizeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(BatchSyncVolumeSizeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }
            @Override
            public void fail(ErrorCode errorCode) {
                BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void saveVolumeProvisioningStrategy(String volumeUuid, VolumeProvisioningStrategy strategy) {
        if (!VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.hasTag(volumeUuid)) {
            SystemTagCreator tagCreator = VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY.newSystemTagCreator(volumeUuid);
            tagCreator.setTagByTokens(
                    map(e(VolumeSystemTags.VOLUME_PROVISIONING_STRATEGY_TOKEN, strategy))
            );
            tagCreator.inherent = false;
            tagCreator.create();
        }
    }

    protected void setCapacity(Long total, Long avail, Long totalPhysical, Long availPhysical) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.update(total, avail, totalPhysical, availPhysical);
    }

    void increaseCapacity(final Long total,
                          final Long avail,
                          final Long totalPhysical,
                          final Long availPhysical,
                          final Long systemUsed) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                if (total != null) {
                    cap.setTotalCapacity(cap.getTotalCapacity() + total);
                }
                if (avail != null) {
                    cap.setAvailableCapacity(cap.getAvailableCapacity() + avail);
                }
                if (totalPhysical != null) {
                    cap.setTotalPhysicalCapacity(cap.getTotalPhysicalCapacity() + totalPhysical);
                }
                if (availPhysical != null) {
                    cap.setAvailablePhysicalCapacity(cap.getAvailablePhysicalCapacity() + availPhysical);
                }
                if (systemUsed != null) {
                    if (cap.getSystemUsedCapacity() == null) {
                        cap.setSystemUsedCapacity(0L);
                    }

                    cap.setSystemUsedCapacity(cap.getSystemUsedCapacity() + systemUsed);
                }
                return cap;
            }
        });
    }

    void decreaseCapacity(final Long total,
                          final Long avail,
                          final Long totalPhysical,
                          final Long availPhysical,
                          final Long systemUsed) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {

                String beforeCapacity = String.format("[totalCapacity: %s, availableCapacity: %s, totalPhysicalCapacity: %s, " +
                                "availablePhysicalCapacity: %s]", cap.getTotalCapacity(), cap.getAvailableCapacity(),
                        cap.getTotalPhysicalCapacity(), cap.getAvailablePhysicalCapacity());

                if (total != null) {
                    long t = cap.getTotalCapacity() - total;
                    cap.setTotalCapacity(t < 0 ? 0 : t);
                }
                if (avail != null) {
                    // for over-provisioning scenarios, minus value of available capacity is permitted
                    long a = cap.getAvailableCapacity() - avail;
                    cap.setAvailableCapacity(a);
                }
                if (totalPhysical != null) {
                    long tp = cap.getTotalPhysicalCapacity() - totalPhysical;
                    cap.setTotalPhysicalCapacity(tp < 0 ? 0 : tp);
                }
                if (availPhysical != null) {
                    long ap = cap.getAvailablePhysicalCapacity() - availPhysical;
                    cap.setAvailablePhysicalCapacity(ap < 0 ? 0 : ap);
                }
                if (systemUsed != null) {
                    long su = cap.getSystemUsedCapacity() - systemUsed;
                    cap.setSystemUsedCapacity(su < 0 ? 0 : su);
                }

                String nowCapacity = String.format("[totalCapacity: %s, availableCapacity: %s, totalPhysicalCapacity: %s, " +
                                "availablePhysicalCapacity: %s]", cap.getTotalCapacity(), cap.getAvailableCapacity(),
                        cap.getTotalPhysicalCapacity(), cap.getAvailablePhysicalCapacity());
                logger.info(String.format("decrease local primary storage[uuid: %s] capacity, changed capacity from %s to %s", cap.getUuid(), beforeCapacity, nowCapacity));

                return cap;
            }
        });
    }


    @Transactional(readOnly = true)
    protected List<FactoryCluster> getAllFactoriesForAttachedClusters() {
        String sql = "select cluster" +
                " from ClusterVO cluster, PrimaryStorageClusterRefVO ref" +
                " where ref.clusterUuid = cluster.uuid" +
                " and ref.primaryStorageUuid = :uuid";
        TypedQuery<ClusterVO> q = dbf.getEntityManager().createQuery(sql, ClusterVO.class);
        q.setParameter("uuid", self.getUuid());
        List<ClusterVO> clusters = q.getResultList();

        if (clusters.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, FactoryCluster> m = new HashMap<>();
        for (ClusterVO c : clusters) {
            FactoryCluster fc = m.get(c.getHypervisorType());
            if (fc == null) {
                fc = new FactoryCluster();
                fc.factory = getHypervisorBackendFactory(c.getHypervisorType());
                fc.clusters = new ArrayList<>();
                m.put(c.getHypervisorType(), fc);
            }

            fc.clusters.add(ClusterInventory.valueOf(c));
        }

        return new ArrayList<FactoryCluster>(m.values());
    }

    @Override
    protected void connectHook(final ConnectParam param, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("connect localstorage host hook");
        chain.then(new NoRollbackFlow() {
            String __name__ = "check localstorage initilized on host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                checkLocalStoragePrimaryStorageInitilized(param.isNewAdded(), true, new Completion(trigger) {
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
            String __name__ = "recaculate primarystorage capacity";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                LocalStorageRecalculateCapacityMsg rmsg = new LocalStorageRecalculateCapacityMsg();
                rmsg.setPrimaryStorageUuid(self.getUuid());
                bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                bus.send(rmsg);
                trigger.next();
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

    private List<HostInventory> getLocalStorageHosts() {
        return new Callable<List<HostInventory>>() {
            @Override
            @Transactional(readOnly = true)
            public List<HostInventory> call() {
                String sql = "select host" +
                        " from LocalStorageHostRefVO h, HostVO host" +
                        " where h.primaryStorageUuid = :puuid" +
                        " and h.hostUuid = host.uuid" +
                        " and host.status != :hstatus";
                TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
                q.setParameter("puuid", self.getUuid());
                q.setParameter("hstatus", HostStatus.Disconnected);
                return HostInventory.valueOf(q.getResultList());
            }
        }.call();
    }

    private void sendWarnning(String hostUuid, String details, PrimaryStorageInventory ps) {
        HostCanonicalEvents.HostMountData data = new HostCanonicalEvents.HostMountData();
        data.hostUuid = hostUuid;
        data.psUuid = ps.getUuid();
        data.details = details;
        eventf.fire(HostCanonicalEvents.HOST_CHECK_INITIALIZED_FAILED, data);
    }

    private boolean hostHasInitializedTag(String hostUuid) {
        List<Map<String, String>> tags = LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.getTokensOfTagsByResourceUuid(hostUuid);
        for (Map<String, String> tag : tags) {
            if (tag.get(LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED_TOKEN).equals(self.getUuid())) {
                return true;
            }
        }
        return false;
    }

    private void checkLocalStoragePrimaryStorageInitilized(boolean isNewAdded, boolean initialized, Completion completion) {
        List<HostInventory> hosts = getLocalStorageHosts();
        if (!isNewAdded && hosts.size() == 0) {
            completion.fail(operr("No Host state is Enabled, Please check the availability of the host"));
        } else {
            checkLocalStoragePrimaryStorageInitilized(hosts, initialized, completion);
        }
    }

    private void checkLocalStoragePrimaryStorageInitilized(List<HostInventory> hosts, boolean initialized, Completion completion) {
        new While<>(hosts).all((host, com) -> {
            if (hostHasInitializedTag(host.getUuid())) {
                LocalStorageHypervisorFactory f = getHypervisorBackendFactory(host.getHypervisorType());
                LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                bkd.checkHostAttachedPSMountPath(host.getUuid(), new ReturnValueCompletion<LocalStorageKvmBackend.CheckInitializedFileRsp>(com) {
                    @Override
                    public void success(LocalStorageKvmBackend.CheckInitializedFileRsp rsp) {
                        if (!rsp.existed) {
                            sendWarnning(host.getUuid(), rsp.getError(), getSelfInventory());
                        }
                        com.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        com.done();
                    }
                });
            } else {
                if (initialized) {
                    LocalStorageHypervisorFactory f = getHypervisorBackendFactory(host.getHypervisorType());
                    LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                    bkd.initializeHostAttachedPSMountPath(host.getUuid(), new Completion(com) {
                        @Override
                        public void success() {
                            SystemTagCreator creator = LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED.newSystemTagCreator(host.getUuid());
                            creator.inherent = true;
                            creator.unique = false;
                            creator.setTagByTokens(map(e(LocalStorageSystemTags.LOCALSTORAGE_HOST_INITIALIZED_TOKEN, self.getUuid())));
                            creator.create();
                            com.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            sendWarnning(host.getUuid(), errorCode.getDetails(), getSelfInventory());
                            com.done();
                        }
                    });
                } else {
                    com.done();
                }
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    protected void pingHook(Completion completion) {
        checkLocalStoragePrimaryStorageInitilized(true, false, completion);
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply) {
               bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeSnapshotEncryptedOnPrimaryStorageReply reply = new GetVolumeSnapshotEncryptedOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    final protected void syncPhysicalCapacity(final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        final List<FactoryCluster> fs = getAllFactoriesForAttachedClusters();
        class Sync {
            long total = 0;
            long avail = 0;
            Iterator<FactoryCluster> it = fs.iterator();

            void sync() {
                if (!it.hasNext()) {
                    PhysicalCapacityUsage ret = new PhysicalCapacityUsage();
                    ret.totalPhysicalSize = total;
                    ret.availablePhysicalSize = avail;
                    completion.success(ret);
                    return;
                }

                FactoryCluster fc = it.next();
                LocalStorageHypervisorBackend bkd = fc.factory.getHypervisorBackend(self);
                bkd.syncPhysicalCapacityInCluster(fc.clusters, new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
                    @Override
                    public void success(PhysicalCapacityUsage returnValue) {
                        total += returnValue.totalPhysicalSize;
                        avail += returnValue.availablePhysicalSize;
                        sync();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        }

        new Sync().sync();
    }

    protected LocalStorageHypervisorFactory getHypervisorBackendFactoryByHostUuid(String hostUuid) {
        return getHypervisorBackendFactoryByHostUuid(hostUuid, true);
    }

    protected LocalStorageHypervisorFactory getHypervisorBackendFactoryByHostUuid(String hostUuid, boolean checkPsRef) {
        if (checkPsRef && !Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, self.getUuid()).isExists()) {
            throw new OperationFailureException(operr("host[uuid:%s] cannot access local storage[uuid:%s], maybe it is detached", hostUuid, self.getUuid()));
        }

        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, hostUuid);
        String hvType = q.findValue();

        return getHypervisorBackendFactory(hvType);
    }

    @Transactional(readOnly = true)
    private LocalStorageHypervisorFactory getHypervisorBackendFactoryByResourceUuid(String resUuid, String resourceType) {
        String sql = "select host.hypervisorType" +
                " from HostVO host, LocalStorageResourceRefVO ref" +
                " where ref.hostUuid = host.uuid" +
                " and ref.resourceUuid = :resUuid" +
                " and ref.primaryStorageUuid = :puuid group by hypervisorType";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new OperationFailureException(
                    operr("resource[uuid:%s, type: %s] is not on the local primary storage[uuid:%s]",
                            resUuid, resourceType, self.getUuid()));
        }
        if (ret.size() != 1) {
            throw new OperationFailureException(
                    operr("resource[uuid:%s, type: %s] on the local primary storage[uuid:%s] maps to multiple hypervisor%s",
                            resUuid, resourceType, self.getUuid(), ret));
        }

        String hvType = ret.get(0);
        return getHypervisorBackendFactory(hvType);
    }

    private LocalStorageHypervisorFactory getHypervisorBackendFactory(String hvType) {
        DebugUtils.Assert(hvType != null, "hvType is null!!!");
        for (LocalStorageHypervisorFactory f : pluginRgty.getExtensionList(LocalStorageHypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find LocalStorageHypervisorFactory with hypervisorType[%s]", hvType));
    }

    @Override
    public void attachHook(final String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }

    @Override
    protected void checkImageIfNeedToDownload(DownloadIsoToPrimaryStorageMsg msg) {
        logger.debug("check if image exist in disabled primary storage");
        if (self.getState() != PrimaryStorageState.Disabled) {
            return;
        }
        if (!Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, msg.getIsoSpec().getInventory().getUuid())
                .like(ImageCacheVO_.installUrl, String.format("%%hostUuid://%s%%", msg.getDestHostUuid()))
                .isExists()) {

            throw new OperationFailureException(operr(
                    "cannot attach ISO to a primary storage[uuid:%s] which is disabled",
                    self.getUuid()));
        }
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        String hvType = Q.New(HostVO.class)
                .select(HostVO_.hypervisorType)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<AskInstallPathForNewSnapshotReply>(msg) {
            @Override
            public void success(AskInstallPathForNewSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
                reply.setSuccess(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        GetPrimaryStorageResourceLocationReply reply = new GetPrimaryStorageResourceLocationReply();
        reply.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        List<String> hostUuids = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, msg.getResourceUuid())
                .select(LocalStorageResourceRefVO_.hostUuid)
                .listValues();
        reply.setHostUuids(hostUuids);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        CheckVolumeSnapshotOperationOnPrimaryStorageReply r = new CheckVolumeSnapshotOperationOnPrimaryStorageReply();
        List<String> disconnectHostUuids = SQL.New("select h.uuid from HostVO h, LocalStorageResourceRefVO ref" +
                " where ref.resourceUuid in :volUuids" +
                " and ref.hostUuid = h.uuid" +
                " and h.status != :hstatus", String.class)
                .param("hstatus", HostStatus.Connected)
                .param("volUuids", msg.getVolumeUuids())
                .list();
        if (!disconnectHostUuids.isEmpty()) {
            r.setError(err(HostErrors.HOST_IS_DISCONNECTED, "host(s)[uuids: %s] volume locate is not Connected.", disconnectHostUuids));
        }

        bus.reply(msg, r);
    }

    private ErrorCode checkChangeVolumeType(String volumeUuid) {
        List<VolumeInventory> refVols = VolumeSnapshotReferenceUtils.getReferenceVolume(volumeUuid);
        if (refVols.isEmpty()) {
            return null;
        }

        List<String> infos = refVols.stream().map(v -> String.format("uuid:%s, name:%s", v.getUuid(), v.getName())).collect(Collectors.toList());
        return operr("volume[uuid:%s] has reference volume[%s], can not change volume type before flatten " +
                "them and their descendants", volumeUuid, infos.toString());
    }

    @Override
    protected void handle(CheckChangeVolumeTypeOnPrimaryStorageMsg msg) {
        CheckChangeVolumeTypeOnPrimaryStorageReply reply = new CheckChangeVolumeTypeOnPrimaryStorageReply();
        ErrorCode errorCode = checkChangeVolumeType(msg.getVolume().getUuid());
        if (errorCode != null) {
            reply.setError(errorCode);;
        }

        bus.reply(msg, reply);
    }

    @Override
    protected void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg) {
        ErrorCode errorCode = checkChangeVolumeType(msg.getVolume().getUuid());
        if (errorCode != null) {
            ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();
            reply.setError(errorCode);
            bus.reply(msg, reply);
            return;
        }

        LocalStorageHypervisorFactory factory = getHypervisorBackendFactoryByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
        factory.getHypervisorBackend(self).handle(msg, new ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ChangeVolumeTypeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ChangeVolumeTypeOnPrimaryStorageReply r = new ChangeVolumeTypeOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }
    @Override
    protected void handle(UnlinkBitsOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory factory = getHypervisorBackendFactoryByResourceUuid(msg.getResourceUuid(), msg.getResourceType());
        factory.getHypervisorBackend(self).handle(msg, new ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(UnlinkBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UnlinkBitsOnPrimaryStorageReply r = new UnlinkBitsOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    public static class LocalStoragePhysicalCapacityUsage extends PrimaryStorageBase.PhysicalCapacityUsage {
        public long localStorageUsedSize;
    }

    @Override
    protected void handle(final UpdateVmInstanceMetadataOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("update-metadata-on-ps-%s", self.getUuid());
            }

            @Override
            public int getSyncLevel() {
                return 10;
            }

            @Override
            public void run(SyncTaskChain chain) {
                final String hostUuid;
                final LocalStorageHypervisorBackend bkd;
                try {
                    hostUuid = getHostUuidByResourceUuid(msg.getRootVolumeUuid());
                    LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                    bkd = f.getHypervisorBackend(self);
                } catch (Exception e) {
                    UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();
                    reply.setError(operr("failed to resolve host for vm metadata update on local primary storage[uuid:%s], rootVolumeUuid[%s]: %s",
                            self.getUuid(), msg.getRootVolumeUuid(), e.getMessage()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                bkd.handle(msg, hostUuid, new ReturnValueCompletion<UpdateVmInstanceMetadataOnPrimaryStorageReply>(msg, chain) {
                    @Override
                    public void success(UpdateVmInstanceMetadataOnPrimaryStorageReply returnValue) {
                        bus.reply(msg, returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        UpdateVmInstanceMetadataOnPrimaryStorageReply reply = new UpdateVmInstanceMetadataOnPrimaryStorageReply();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("update-metadata-on-ps-%s", self.getUuid());
            }
        });
    }

    @Override
    protected void handle(final GetVmInstanceMetadataFromPrimaryStorageMsg msg) {
        GetVmInstanceMetadataFromPrimaryStorageReply reply = new GetVmInstanceMetadataFromPrimaryStorageReply();

        String hostUuid = null;
        if (msg.getHostUuid() != null) {
            hostUuid = msg.getHostUuid();
        }

        if (hostUuid == null) {
            if (msg.getRootVolumeUuid() == null) {
                reply.setError(operr("cannot determine host for vm metadata get on local primary storage[uuid:%s]," +
                        " rootVolumeUuid is null", self.getUuid()));
                bus.reply(msg, reply);
                return;
            }
            try {
                hostUuid = getHostUuidByResourceUuid(msg.getRootVolumeUuid());
            } catch (Exception e) {
                reply.setError(operr("cannot determine host for vm metadata get on local primary storage[uuid:%s], rootVolumeUuid[%s]: %s",
                        self.getUuid(), msg.getRootVolumeUuid(), e.getMessage()));
                bus.reply(msg, reply);
                return;
            }
        }

        final LocalStorageHypervisorBackend bkd;
        try {
            LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
            bkd = f.getHypervisorBackend(self);
        } catch (Exception e) {
            reply.setError(operr("failed to resolve host/backend for vm metadata get on local primary storage[uuid:%s], hostUuid[%s]: %s",
                    self.getUuid(), hostUuid, e.getMessage()));
            bus.reply(msg, reply);
            return;
        }
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<GetVmInstanceMetadataFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVmInstanceMetadataFromPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final ScanVmInstanceMetadataFromPrimaryStorageMsg msg) {
        ScanVmInstanceMetadataFromPrimaryStorageReply reply = new ScanVmInstanceMetadataFromPrimaryStorageReply();

        List<String> allHostUuids = SQL.New(
                        "select h.hostUuid from LocalStorageHostRefVO h, HostVO host" +
                                " where h.primaryStorageUuid = :psUuid" +
                                " and h.hostUuid = host.uuid", String.class)
                .param("psUuid", self.getUuid())
                .list();
        if (allHostUuids.isEmpty()) {
            reply.setError(operr("no host found for local primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<String> connectedHostUuids = SQL.New(
                        "select h.hostUuid from LocalStorageHostRefVO h, HostVO host" +
                                " where h.primaryStorageUuid = :psUuid" +
                                " and h.hostUuid = host.uuid" +
                                " and host.status = :hstatus", String.class)
                .param("psUuid", self.getUuid())
                .param("hstatus", HostStatus.Connected)
                .list();
        if (connectedHostUuids.isEmpty()) {
            reply.setError(operr("no connected host found for local primary storage[uuid:%s], " +
                    "total hosts=%d", self.getUuid(), allHostUuids.size()));
            bus.reply(msg, reply);
            return;
        }

        // Disconnected hosts cannot be scanned; treat them as failed upfront
        List<String> disconnectedHostUuids = new ArrayList<>(allHostUuids);
        disconnectedHostUuids.removeAll(connectedHostUuids);

        List<VmMetadataScanEntry> allSummaries = Collections.synchronizedList(new ArrayList<>());
        List<String> failedHosts = Collections.synchronizedList(new ArrayList<>(disconnectedHostUuids));
        if (!disconnectedHostUuids.isEmpty()) {
            logger.warn(String.format("local primary storage[uuid:%s] has %d disconnected hosts that cannot be scanned: %s",
                    self.getUuid(), disconnectedHostUuids.size(), disconnectedHostUuids));
        }

        new While<>(connectedHostUuids).all((hostUuid, com) -> {
            final LocalStorageHypervisorBackend bkd;
            try {
                LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                bkd = f.getHypervisorBackend(self);
            } catch (Exception e) {
                failedHosts.add(hostUuid);
                com.addError(operr("failed to prepare vm metadata scan on host[uuid:%s]: %s",
                        hostUuid, e.getMessage()));
                com.done();
                return;
            }
            bkd.handle(msg, hostUuid, new ReturnValueCompletion<ScanVmInstanceMetadataFromPrimaryStorageReply>(com) {
                @Override
                public void success(ScanVmInstanceMetadataFromPrimaryStorageReply returnValue) {
                    if (returnValue.getVmInstanceMetadata() != null) {
                        allSummaries.addAll(returnValue.getVmInstanceMetadata());
                    }
                    com.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to scan vm metadata from host[uuid:%s] on local primary storage[uuid:%s]: %s",
                            hostUuid, self.getUuid(), errorCode));
                    failedHosts.add(hostUuid);
                    com.addError(errorCode);
                    com.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty() && errorCodeList.getCauses().size() == connectedHostUuids.size()) {
                    reply.setError(operr("failed to scan vm metadata from all hosts on local primary storage[uuid:%s], causes: %s",
                            self.getUuid(), errorCodeList));
                } else {
                    enrichLocalVmMetadataRegistrationHints(allSummaries);
                    reply.setVmInstanceMetadata(new ArrayList<>(allSummaries));
                    if (!failedHosts.isEmpty()) {
                        logger.warn(String.format("partial scan failure on local primary storage[uuid:%s], " +
                                "failedHosts=%s, successHosts=%d, totalHosts=%d (connected=%d, disconnected=%d)",
                                self.getUuid(), failedHosts,
                                allHostUuids.size() - failedHosts.size(), allHostUuids.size(),
                                connectedHostUuids.size(), disconnectedHostUuids.size()));
                    }
                }
                bus.reply(msg, reply);
            }
        });
    }

    private void enrichLocalVmMetadataRegistrationHints(List<VmMetadataScanEntry> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        Set<String> vmUuids = metadata.stream()
                .map(VmMetadataScanEntry::getVmUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (vmUuids.isEmpty()) {
            return;
        }

        Map<String, String> vmRootVolumeUuids = new HashMap<>();
        List<Tuple> volumeTuples = Q.New(VolumeVO.class)
                .select(VolumeVO_.vmInstanceUuid, VolumeVO_.uuid)
                .in(VolumeVO_.vmInstanceUuid, vmUuids)
                .eq(VolumeVO_.primaryStorageUuid, self.getUuid())
                .eq(VolumeVO_.type, VolumeType.Root)
                .listTuple();
        for (Tuple tuple : volumeTuples) {
            vmRootVolumeUuids.put(tuple.get(0, String.class), tuple.get(1, String.class));
        }
        if (vmRootVolumeUuids.isEmpty()) {
            return;
        }

        Map<String, String> rootVolumeHostUuids = new HashMap<>();
        List<Tuple> refTuples = Q.New(LocalStorageResourceRefVO.class)
                .select(LocalStorageResourceRefVO_.resourceUuid, LocalStorageResourceRefVO_.hostUuid)
                .in(LocalStorageResourceRefVO_.resourceUuid, vmRootVolumeUuids.values())
                .eq(LocalStorageResourceRefVO_.primaryStorageUuid, self.getUuid())
                .listTuple();
        for (Tuple tuple : refTuples) {
            rootVolumeHostUuids.put(tuple.get(0, String.class), tuple.get(1, String.class));
        }

        for (VmMetadataScanEntry entry : metadata) {
            String rootVolumeUuid = vmRootVolumeUuids.get(entry.getVmUuid());
            String existingHostUuid = rootVolumeHostUuids.get(rootVolumeUuid);
            if (existingHostUuid != null && entry.getHostUuid() != null
                    && !Objects.equals(existingHostUuid, entry.getHostUuid())) {
                entry.setRegenerateUuidRequired(true);
            }
        }
    }

    @Override
    protected void handle(final CleanupVmInstanceMetadataOnPrimaryStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("update-metadata-on-ps-%s-%s", self.getUuid(), msg.getVmInstanceUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public void run(SyncTaskChain chain) {
                CleanupVmInstanceMetadataOnPrimaryStorageReply reply = new CleanupVmInstanceMetadataOnPrimaryStorageReply();

                String hostUuid = msg.getHostUuid();
                if (hostUuid == null && msg.getRootVolumeUuid() != null) {
                    try {
                        hostUuid = getHostUuidByResourceUuid(msg.getRootVolumeUuid());
                    } catch (Exception e) {
                        logger.warn(String.format("failed to get host by rootVolumeUuid[%s]: %s", msg.getRootVolumeUuid(), e.getMessage()));
                    }
                }

                if (hostUuid == null) {
                    reply.setError(operr("cannot determine host for vm metadata cleanup on local primary storage[uuid:%s]," +
                            " rootVolumeUuid[%s], hostUuid is null", self.getUuid(), msg.getRootVolumeUuid()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                final LocalStorageHypervisorBackend bkd;
                try {
                    LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                    bkd = f.getHypervisorBackend(self);
                } catch (Exception e) {
                    reply.setError(operr("failed to resolve host/backend for vm metadata cleanup on local primary storage[uuid:%s], hostUuid[%s]: %s",
                            self.getUuid(), hostUuid, e.getMessage()));
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }
                bkd.handle(msg, hostUuid, new ReturnValueCompletion<CleanupVmInstanceMetadataOnPrimaryStorageReply>(msg, chain) {
                    @Override
                    public void success(CleanupVmInstanceMetadataOnPrimaryStorageReply returnValue) {
                        bus.reply(msg, returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("cleanup-metadata-on-ps-%s-%s", self.getUuid(), msg.getVmInstanceUuid());
            }
        });
    }

    @Override
    protected void handle(final CleanupAllVmMetadataOnPrimaryStorageMsg msg) {
        CleanupAllVmMetadataOnPrimaryStorageReply reply = new CleanupAllVmMetadataOnPrimaryStorageReply();

        List<String> allHostUuids = getLocalStorageHostUuids();
        if (allHostUuids.isEmpty()) {
            reply.setError(operr("no host found for local primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<String> connectedHostUuids = getConnectedLocalStorageHostUuids();
        if (connectedHostUuids.isEmpty()) {
            reply.setError(operr("no connected host found for local primary storage[uuid:%s], " +
                    "total hosts=%d", self.getUuid(), allHostUuids.size()));
            bus.reply(msg, reply);
            return;
        }

        List<String> disconnectedHostUuids = new ArrayList<>(allHostUuids);
        disconnectedHostUuids.removeAll(connectedHostUuids);
        if (!disconnectedHostUuids.isEmpty()) {
            logger.warn(String.format("local primary storage[uuid:%s] has %d disconnected hosts that cannot clean up vm metadata: %s",
                    self.getUuid(), disconnectedHostUuids.size(), disconnectedHostUuids));
        }

        new While<>(connectedHostUuids).all((hostUuid, com) -> {
            final LocalStorageHypervisorBackend bkd;
            try {
                LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                bkd = f.getHypervisorBackend(self);
            } catch (Exception e) {
                logger.warn(String.format("[MetadataCleanupAll] failed to prepare backend for host[uuid:%s] on ps[uuid:%s]: %s",
                        hostUuid, self.getUuid(), e.getMessage()));
                com.addError(operr("host[uuid:%s] backend prepare failed: %s", hostUuid, e.getMessage()));
                com.done();
                return;
            }

            bkd.handle(msg, hostUuid, new ReturnValueCompletion<CleanupAllVmMetadataOnPrimaryStorageReply>(com) {
                @Override
                public void success(CleanupAllVmMetadataOnPrimaryStorageReply returnValue) {
                    if (returnValue.isSkipped()) {
                        synchronized (reply) {
                            reply.setSkipped(true);
                            if (returnValue.getCurrentGeneration() != null
                                    && (reply.getCurrentGeneration() == null
                                    || returnValue.getCurrentGeneration() > reply.getCurrentGeneration())) {
                                reply.setCurrentGeneration(returnValue.getCurrentGeneration());
                            }
                        }
                    }
                    com.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("[MetadataCleanupAll] failed on host[uuid:%s] on local ps[uuid:%s]: %s",
                            hostUuid, self.getUuid(), errorCode));
                    com.addError(errorCode);
                    com.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                int failedHostCount = disconnectedHostUuids.size() + errorCodeList.getCauses().size();
                if (failedHostCount > 0) {
                    reply.setError(operr("local primary storage[uuid:%s] cleanup all vm metadata failed on %d/%d host(s), " +
                                    "disconnectedHosts=%s, causes=%s",
                            self.getUuid(), failedHostCount, allHostUuids.size(), disconnectedHostUuids, errorCodeList));
                }
                bus.reply(msg, reply);
            }
        });
    }

    private List<String> getLocalStorageHostUuids() {
        return SQL.New(
                        "select h.hostUuid from LocalStorageHostRefVO h, HostVO host" +
                                " where h.primaryStorageUuid = :psUuid" +
                                " and h.hostUuid = host.uuid", String.class)
                .param("psUuid", self.getUuid())
                .list();
    }

    private List<String> getConnectedLocalStorageHostUuids() {
        return SQL.New(
                        "select h.hostUuid from LocalStorageHostRefVO h, HostVO host" +
                                " where h.primaryStorageUuid = :psUuid" +
                                " and h.hostUuid = host.uuid" +
                                " and host.status = :hstatus", String.class)
                .param("psUuid", self.getUuid())
                .param("hstatus", HostStatus.Connected)
                .list();
    }
}
