package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.ShrinkVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.*;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMTakeSnapshotExtensionPoint;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.snapshot.reference.VolumeSnapshotReferenceUtils;
import org.zstack.storage.volume.VolumeErrors;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.primary.smp.SMPPrimaryStorageFactory.type;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/3/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SMPPrimaryStorageBase extends PrimaryStorageBase implements KVMTakeSnapshotExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SMPPrimaryStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private SMPPrimaryStorageImageCacheCleaner imageCacheCleaner;

    public SMPPrimaryStorageBase() {
    }

    public SMPPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    private HypervisorFactory getHypervisorFactoryByHypervisorType(String hvType) {
        return getHypervisorFactoryByHypervisorAndExtensionType(hvType, null);
    }

    protected HypervisorFactory getHypervisorFactoryByHypervisorAndExtensionType(String hvType, String extensionType) {
        for (HypervisorFactory f : pluginRgty.getExtensionList(HypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType()) && Objects.equals(f.getExtensionType(), extensionType)) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find HypervisorFactory[hypervisroType = %s, extensionType = %s]", hvType, extensionType));
    }

    protected HypervisorFactory getHypervisorFactoryByHostUuid(String huuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, huuid);
        String hvType = q.findValue();
        return getHypervisorFactoryByHypervisorType(hvType);
    }

    protected String getHypervisorTypeByClusterUuid(String cuuid) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, cuuid);
        return q.findValue();
    }

    protected HypervisorFactory getHypervisorFactoryByClusterUuid(String cuuid) {
        String hvType = getHypervisorTypeByClusterUuid(cuuid);
        return getHypervisorFactoryByHypervisorType(hvType);
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(clusterUuid).getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }

    @Override
    protected void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getTemplateSpec().getInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadVolumeTemplateToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadVolumeTemplateToPrimaryStorageReply reply = new DownloadVolumeTemplateToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg.getDestHost() == null) {
            String hostUuid = getAvailableHostUuidForOperation();
            if (hostUuid == null) {
                throw new OperationFailureException(operr("the shared mount point primary storage[uuid:%s, name:%s] cannot find any " +
                                "available host in attached clusters for instantiating the volume", self.getUuid(), self.getName()));
            }

            msg.setDestHost(HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class)));
        }

        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHost().getUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(InstantiateVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = findHypervisorTypeByImageFormatAndPrimaryStorageUuid(msg.getVolume().getFormat(), msg.getPrimaryStorageUuid());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeOnPrimaryStorageReply reply) {
                logger.debug( String.format("successfully delete volume[uuid:%s]", msg.getVolume().getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("unable to delete path:%s right now, skip this GC job because it's in use", msg.getVolume().getInstallPath()));
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                    return;
                }

                logger.debug( String.format("can't delete volume[uuid:%s] right now, add a GC job", msg.getVolume().getUuid()));
                SMPDeleteVolumeGC gc = new SMPDeleteVolumeGC();
                gc.NAME = String.format("gc-smp-%s-volume-%s", self.getUuid(), msg.getVolume().getUuid());
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = type.toString();
                gc.volume = msg.getVolume();
                gc.deduplicateSubmit(SMPPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type);
        final HypervisorBackend bkd = f.getHypervisorBackend(self);

        bkd.handle(msg, new ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteBitsOnPrimaryStorageReply reply) {
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
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeSnapshot().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadDataVolumeToPrimaryStorageReply reply) {
                saveVolumeProvisioningStrategy(msg.getVolumeUuid(), VolumeProvisioningStrategy.ThinProvisioning);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply>(msg) {
            @Override
            public void success(GetInstallPathForDataVolumeDownloadReply reply) {
                bus.reply(msg, reply);
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
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadIsoToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getIsoSpec().getInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteIsoFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
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
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        String format = q.findValue();

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
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
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        String format = q.findValue();

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply>(msg) {
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
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();
        bus.reply(msg, reply);
        logger.warn("Not supported at current edition");
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

    protected void hookToKVMHostConnectedEventToChangeStatusToConnected(){
        // hook on host connected event to reconnect the primary storage once there is
        // a host connected in attached clusters
        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new AutoOffEventCallback() {
            {
                uniqueIdentity = String.format("connect-smp-%s-when-host-connected", self.getUuid());
            }

            @Override
            protected boolean run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!HostStatus.Connected.toString().equals(d.getNewStatus())) {
                    return false;
                }

                if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(d.getInventory().getHypervisorType())) {
                    return false;
                }

                self = dbf.reload(self);
                if (self.getStatus() == PrimaryStorageStatus.Connected) {
                    return true;
                }

                if (!self.getAttachedClusterRefs().stream()
                        .anyMatch(ref -> ref.getClusterUuid().equals(d.getInventory().getClusterUuid()))) {
                    return false;
                }

                FutureCompletion future = new FutureCompletion(null);

                ConnectParam p = new ConnectParam();
                p.setNewAdded(false);
                connectHook(p, future);

                future.await();

                if (!future.isSuccess()) {
                    logger.warn(String.format("unable to reconnect the primary storage[uuid:%s, name:%s], %s",
                            self.getUuid(), self.getName(), future.getErrorCode()));
                } else {
                    changeStatus(PrimaryStorageStatus.Connected);
                }

                return future.isSuccess();
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        List<String> clusterUuids = self.getAttachedClusterRefs().stream()
                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                .collect(Collectors.toList());

        if (!clusterUuids.isEmpty()) {
            clusterUuids = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .in(HostVO_.clusterUuid, clusterUuids)
                    .listValues();
        }

        if (clusterUuids.isEmpty()){
            if (!param.isNewAdded()){
                hookToKVMHostConnectedEventToChangeStatusToConnected();
            }

            completion.fail(err(PrimaryStorageErrors.DISCONNECTED,
                    "the SMP primary storage[uuid:%s, name:%s] has not attached to any clusters, " +
                            "or no hosts in the attached clusters are connected", self.getUuid(), self.getName()
            ));
            return;
        }

        final Set<String> finalClusterUuids = new HashSet<>(clusterUuids);
        new LoopAsyncBatch<String>(completion) {
            boolean success;

            @Override
            protected Collection<String> collect() {
                return finalClusterUuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String item) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(item).getHypervisorBackend(self);
                        bkd.connectByClusterUuid(item, new ReturnValueCompletion<ClusterConnectionStatus>(completion) {
                            @Override
                            public void success(ClusterConnectionStatus clusterStatus) {
                                // isConnectedHostInCluster has been checked before
                                success = true;
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errors.add(errorCode);
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (success) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to connect to all clusters%s", finalClusterUuids), errors
                    ));
                }
            }
        }.start();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        completion.fail(operr("not supported operation"));
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        VolumeSnapshotVO snapshotVO = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);

        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(snapshotVO.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeSnapshotEncryptedOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
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
    public void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof CheckSnapshotMsg) {
            handle((CheckSnapshotMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof SMPPrimaryStorageHypervisorSpecificMessage) {
            handle((SMPPrimaryStorageHypervisorSpecificMessage) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof SMPRecalculatePrimaryStorageCapacityMsg) {
            handle((SMPRecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((DownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if (msg instanceof CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) {
            handle((CancelDownloadBitsFromKVMHostToPrimaryStorageMsg) msg);
        } else if ((msg instanceof GetDownloadBitsFromKVMHostProgressMsg)) {
            handle((GetDownloadBitsFromKVMHostProgressMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CommitVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CommitVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof PullVolumeSnapshotOnPrimaryStorageMsg) {
            handle((PullVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }
    private void handle(final DownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadBitsFromKVMHostToPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadBitsFromKVMHostToPrimaryStorageReply reply = new DownloadBitsFromKVMHostToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg) {
        CancelDownloadBitsFromKVMHostToPrimaryStorageReply reply = new CancelDownloadBitsFromKVMHostToPrimaryStorageReply();
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new Completion(msg) {
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

    private void handle(final GetDownloadBitsFromKVMHostProgressMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply>(msg) {
            public void success(GetDownloadBitsFromKVMHostProgressReply returnValue) {
                logger.info(String.format("successfully get downloaded bits progress from primary storage %s", msg.getPrimaryStorageUuid()));
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to get downloaded bits progress from primary storage %s", msg.getPrimaryStorageUuid()));
                GetDownloadBitsFromKVMHostProgressReply reply = new GetDownloadBitsFromKVMHostProgressReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        HypervisorBackend bkd;
        if (msg.getHostUuid() == null) {
            bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeUuid());
        } else {
            HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
            bkd = f.getHypervisorBackend(self);
        }

        bkd.handle(msg, new ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply>(msg) {
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

    private void handle(ResizeVolumeOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<ResizeVolumeOnPrimaryStorageReply>(msg) {

            @Override
            public void success(ResizeVolumeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }


    private void handle(final DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply sreply = new DeleteImageCacheOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-cache-on-smp-primary-storage-%s", msg.getPrimaryStorageUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-cache";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String hostUuid = getAvailableHostUuidForOperation();
                if (hostUuid == null) {
                    trigger.next();
                    return;
                }
                HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
                DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                dmsg.setFolder(true);
                dmsg.setHypervisorType(hvo.getHypervisorType());
                dmsg.setInstallPath(new File(msg.getInstallPath()).getParent());
                dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, sreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                sreply.setError(errCode);
                bus.reply(msg, sreply);
            }
        }).start();
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    protected void handle(SMPRecalculatePrimaryStorageCapacityMsg msg) {
        if (msg.isRelease()) {
            doReleasePrimaryStorageCapacity();
        } else {
            RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(self.getUuid());
            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(rmsg);
        }
    }

    private void doReleasePrimaryStorageCapacity() {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setAvailableCapacity(0L);
                cap.setTotalCapacity(0L);
                cap.setTotalPhysicalCapacity(0L);
                cap.setAvailablePhysicalCapacity(0L);
                cap.setSystemUsedCapacity(0L);
                return cap;
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
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

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<UploadBitsToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsToBackupStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SMPPrimaryStorageHypervisorSpecificMessage msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handleHypervisorSpecificMessage(msg);
    }

    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getTo().getUuid());
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        bkd.stream(msg.getFrom(), msg.getTo(), msg.isFullRebase(), new Completion(msg) {
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

    @Override
    protected void handle(FlattenVolumeOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        bkd.stream(null, msg.getVolume(), true, new Completion(msg) {
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

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply>(msg) {
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
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ReInitRootVolumeFromTemplateOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected HypervisorBackend getHypervisorBackendByVolumeUuid(String volUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, Op.EQ, volUuid);
        String format = q.findValue();

        if (format == null) {
            throw new CloudRuntimeException(String.format("cannot find the volume[uuid:%s]", volUuid));
        }

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        return f.getHypervisorBackend(self);
    }

    private void handle(final CheckSnapshotMsg msg) {
        CheckSnapshotReply reply = new CheckSnapshotReply();
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeUuid());
        bkd.handle(msg, new Completion(msg) {
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
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(sp.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<TakeSnapshotReply>(msg) {
            @Override
            public void success(TakeSnapshotReply returnValue) {
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

    @Transactional(readOnly = true)
    protected String getAvailableHostUuidForOperation() {
        String sql = "select host.uuid from PrimaryStorageClusterRefVO ref, HostVO host where" +
                " ref.clusterUuid = host.clusterUuid and ref.primaryStorageUuid = :psUuid and host.status = :hstatus" +
                " and host.state = :hstate";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", self.getUuid());
        q.setParameter("hstatus", HostStatus.Connected);
        q.setParameter("hstate", HostState.Enabled);
        List<String> hostUuids = q.getResultList();
        if (hostUuids.isEmpty()) {
            return null;
        }

        Collections.shuffle(hostUuids);
        return hostUuids.get(0);
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeInventory().getUuid());

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
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    public HypervisorType findHypervisorTypeByImageFormatAndPrimaryStorageUuid(String imageFormat, final String psUuid) {
        HypervisorType hvType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(imageFormat);
        if (hvType != null) {
            return hvType;
        }

        String type = new Callable<String>() {
            @Override
            @Transactional(readOnly = true)
            public String call() {
                String sql = "select c.hypervisorType" +
                        " from ClusterVO c, PrimaryStorageClusterRefVO ref" +
                        " where c.uuid = ref.clusterUuid" +
                        " and ref.primaryStorageUuid = :psUuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("psUuid", psUuid);
                List<String> types = q.getResultList();
                return types.isEmpty() ? null : types.get(0);
            }
        }.call();

        if (type != null) {
            return HypervisorType.valueOf(type);
        }

        throw new OperationFailureException(operr("cannot find proper hypervisorType for primary storage[uuid:%s] to handle image format or volume format[%s]", psUuid, imageFormat));
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        CheckVolumeSnapshotOperationOnPrimaryStorageReply reply = new CheckVolumeSnapshotOperationOnPrimaryStorageReply();
        if (msg.getVmInstanceUuid() != null) {
            HostStatus hostStatus = SQL.New("select host.status from VmInstanceVO vm, HostVO host" +
                    " where vm.uuid = :vmUuid" +
                    " and vm.hostUuid = host.uuid", HostStatus.class)
                    .param("vmUuid", msg.getVmInstanceUuid())
                    .find();
            if (hostStatus == null && getAvailableHostUuidForOperation() == null) {
                reply.setError(err(HostErrors.HOST_IS_DISCONNECTED, "cannot find available host for operation on" +
                        " primary storage[uuid:%s].", self.getUuid()));
            } else if (hostStatus != HostStatus.Connected && hostStatus != null) {
                reply.setError(err(HostErrors.HOST_IS_DISCONNECTED, "host where vm[uuid:%s] locate is not Connected.", msg.getVmInstanceUuid()));
            }
        }

        bus.reply(msg, reply);
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

        HypervisorBackend backend = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        backend.handle(msg, new ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ChangeVolumeTypeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ChangeVolumeTypeOnPrimaryStorageReply reply = new ChangeVolumeTypeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(UnlinkBitsOnPrimaryStorageMsg msg) {
        HypervisorBackend backend = getHypervisorBackendByVolumeUuid(msg.getResourceUuid());
        backend.handle(msg, new ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(UnlinkBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UnlinkBitsOnPrimaryStorageReply reply = new UnlinkBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CommitVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CommitVolumeSnapshotOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
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
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(PullVolumeSnapshotOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                PullVolumeSnapshotOnPrimaryStorageReply reply = new PullVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private boolean isSharedMountPointPrimaryStorage(String psUuid) {
        return psUuid != null && Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, type.toString())
                .isExists();
    }

    @Override
    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd scmd, Completion completion) {
        boolean needPreCreateVolume = scmd.isOnline();
        if (!needPreCreateVolume || !isSharedMountPointPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            completion.success();
            return;
        }

        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(dbf.findByUuid(msg.getVolume().getPrimaryStorageUuid(), PrimaryStorageVO.class));

        VolumeInventory volumeInventory = new VolumeInventory(msg.getVolume());
        volumeInventory.setInstallPath(scmd.getInstallPath());
        volumeInventory.setName(msg.getSnapshotName());
        bkd.createEmptyVolumeWithBackingFile(volumeInventory, msg.getHostUuid(), msg.getVolume().getInstallPath(), new ReturnValueCompletion<KvmBackend.AgentRsp>(completion) {
            @Override
            public void success(KvmBackend.AgentRsp rsp) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp) {
    }

    @Override
    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg, KVMAgentCommands.TakeSnapshotCmd cmd, KVMAgentCommands.TakeSnapshotResponse rsp, ErrorCode err) {
        boolean needPreCreateVolume = cmd.isOnline();
        if (!needPreCreateVolume || !isSharedMountPointPrimaryStorage(msg.getVolume().getPrimaryStorageUuid())) {
            return;
        }
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(dbf.findByUuid(msg.getVolume().getPrimaryStorageUuid(), PrimaryStorageVO.class));
        bkd.deleteBits(cmd.getInstallPath(), new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("successfully cleaned garbage snapshot volume[name: %s, installpath:%s] for take snapshot on volume[%s]",
                        msg.getSnapshotName(), cmd.getInstallPath(), msg.getVolume().getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (errorCode.isError(VolumeErrors.VOLUME_IN_USE)) {
                    logger.debug(String.format("unable to delete path:%s right now, skip this GC job because it's in use", cmd.getInstallPath()));
                    return;
                }
                logger.debug(String.format("failed to clean garbage snapshot volume[name: %s, installpath:%s] for failed taking snapshot on volume[%s], "+
                        "create gc job to clean garbage late", msg.getSnapshotName(), cmd.getInstallPath(), msg.getVolume().getUuid()));
                SMPDeleteVolumeGC gc = new SMPDeleteVolumeGC();
                gc.NAME = String.format("gc-smp-%s-snapshot-%s", msg.getVolume().getPrimaryStorageUuid(), cmd.getInstallPath());
                gc.primaryStorageUuid = msg.getVolume().getPrimaryStorageUuid();
                gc.hypervisorType = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat()).toString();
                VolumeInventory inv = new VolumeInventory(msg.getVolume());
                inv.setInstallPath(cmd.getInstallPath());
                gc.volume = inv;
                gc.submit(SMPPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
            }
        });
    }
}
