package org.zstack.storage.addon.primary;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.trash.TrashType;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.addon.RemoteTarget;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.header.volume.block.BlockVolumeVO;
import org.zstack.header.volume.block.BlockVolumeVO_;
import org.zstack.header.volume.block.GetAccessPathMsg;
import org.zstack.header.volume.block.GetAccessPathReply;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.backup.BackupStorageSystemTags;
import org.zstack.storage.primary.*;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.storage.addon.primary.ExternalPrimaryStorageNameHelper.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public class ExternalPrimaryStorage extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorage.class);
    private static final Logger log = LoggerFactory.getLogger(ExternalPrimaryStorage.class);

    protected final PrimaryStorageNodeSvc node;
    protected final PrimaryStorageControllerSvc controller;

    private ExternalPrimaryStorageVO externalVO;
    private LinkedHashMap selfConfig;

    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected ResourceConfigFacade rcf;
    @Autowired
    private ExternalPrimaryStorageImageCacheCleaner imageCacheCleaner;
    @Autowired
    protected ExternalPrimaryStorageFactory factory;

    @Autowired
    private ResourceDestinationMaker destMaker;

    public ExternalPrimaryStorage(PrimaryStorageVO self, PrimaryStorageControllerSvc controller, PrimaryStorageNodeSvc node) {
        super(self);
        this.controller = controller;
        this.node = node;
        this.externalVO = self instanceof ExternalPrimaryStorageVO ? (ExternalPrimaryStorageVO) self : Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                .find();
        this.selfConfig = JSONObjectUtil.toObject(externalVO.getConfig(), LinkedHashMap.class);
    }

    public ExternalPrimaryStorage(ExternalPrimaryStorage other) {
        super(other.externalVO);
        this.controller = other.controller;
        this.node = other.node;
        this.externalVO = other.externalVO;
        this.selfConfig = other.selfConfig;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof PrimaryStorageMessage && !destMaker.isManagedByUs(((PrimaryStorageMessage) msg).getPrimaryStorageUuid())) {
            logger.warn(String.format("message[%s] is not managed by us, we may not has ps controller for it, " +
                    "please contact us ASAP.", msg.getClass().getName()));
        }

        super.handleMessage(msg);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof SelectBackupStorageMsg) {
            handle((SelectBackupStorageMsg) msg);
        } else if (msg instanceof SetVolumeQosOnPrimaryStorageMsg) {
            handle((SetVolumeQosOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeQosOnPrimaryStorageMsg) {
            handle((DeleteVolumeQosOnPrimaryStorageMsg) msg);
        } else if (msg instanceof ResizeVolumeOnPrimaryStorageMsg) {
            handle((ResizeVolumeOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof SetTrashExpirationTimeMsg) {
            handle((SetTrashExpirationTimeMsg) msg);
        } else if (msg instanceof GetAccessPathMsg) {
            handle((GetAccessPathMsg) msg);
        } else if (msg instanceof UpdatePrimaryStorageHostStatusMsg) {
            handle((UpdatePrimaryStorageHostStatusMsg) msg);
        } else if (msg instanceof GetVolumeBackingChainFromPrimaryStorageMsg) {
            handle((GetVolumeBackingChainFromPrimaryStorageMsg) msg);
        } else if (msg instanceof DeleteVolumeChainOnPrimaryStorageMsg) {
            handle((DeleteVolumeChainOnPrimaryStorageMsg) msg);
        } else if (msg instanceof EncryptVolumeBitsOnPrimaryStorageMsg) {
            handle((EncryptVolumeBitsOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(GetAccessPathMsg msg) {
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (blockVolumeVO == null) {
            GetAccessPathReply reply = new GetAccessPathReply();
            reply.setError(operr("can not found block volume, access path only for block volume"));
            bus.reply(msg, reply);
            return;
        }
        BlockExternalPrimaryStorageBackend backend = getBlockBackend(blockVolumeVO.getVendor());
        backend.handle(msg);
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateExternalPrimaryStorageMsg) {
            handle((APIUpdateExternalPrimaryStorageMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private void handle(APIUpdateExternalPrimaryStorageMsg msg) {
        APIUpdateExternalPrimaryStorageEvent evt = new APIUpdateExternalPrimaryStorageEvent(msg.getId());
        if (msg.getName() != null) {
            externalVO.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            externalVO.setDescription(msg.getDescription());
        }
        if (msg.getUrl() != null) {
            externalVO.setUrl(msg.getUrl());
        }
        if (msg.getDefaultProtocol() != null) {
            externalVO.setDefaultProtocol(msg.getDefaultProtocol());
        }
        boolean needReconnect = false;
        if (msg.getConfig() != null) {
            controller.validateConfig(msg.getConfig());
            externalVO.setConfig(msg.getConfig());
            needReconnect = true;
        }
        externalVO = dbf.updateAndRefresh(externalVO);

        if (needReconnect) {
            ReconnectPrimaryStorageMsg rmsg = new ReconnectPrimaryStorageMsg();
            rmsg.setPrimaryStorageUuid(externalVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, rmsg.getPrimaryStorageUuid());
            bus.send(rmsg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        evt.setError(reply.getError());
                    } else {
                        self = dbf.reload(self);
                        evt.setInventory(externalVO.toInventory());
                    }

                    bus.publish(evt);
                }
            });
            return;
        }

        evt.setInventory(externalVO.toInventory());
        bus.publish(evt);
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid(), false);
        bus.publish(evt);
    }

    @Override
    protected void handle(UpdatePrimaryStorageHostStatusMsg msg) {
        ExternalPrimaryStorageHostRefVO ref = Q.New(ExternalPrimaryStorageHostRefVO.class)
                .eq(ExternalPrimaryStorageHostRefVO_.hostUuid, msg.getHostUuid())
                .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .find();

        if (ref == null || ref.getHostId() == 0) {
            // FIXME: different primary storage may have different max host id
            ref = new ExternalHostIdGetter(999).getOrAllocateHostIdRef(msg.getHostUuid(), msg.getPrimaryStorageUuid());
        }

        updatePrimaryStorageHostStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), msg.getStatus(), msg.getReason());
        UpdatePrimaryStorageHostStatusReply reply = new UpdatePrimaryStorageHostStatusReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(InstantiateVolumeOnPrimaryStorageMsg msg) {
        VolumeInventory volume = msg.getVolume();
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, volume.getUuid()).find();
        if (blockVolumeVO != null) {
            BlockExternalPrimaryStorageBackend backend = getBlockBackend(blockVolumeVO.getVendor());
            backend.handle(msg);
            return;
        }

        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setUuid(volume.getUuid());
        spec.setSize(volume.getSize());
        spec.setAllocatedUrl(msg.getAllocatedInstallUrl());
        spec.setEncrypted(Boolean.TRUE.equals(volume.getEncrypted()));
        if (msg instanceof InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) {
            // FIXME: use more accurate name
            spec.setName(buildReimageVolumeName(((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) msg).getOriginVolumeUuid()));
            createRootVolume((InstantiateTemporaryRootVolumeFromTemplateOnPrimaryStorageMsg) msg, spec);
        } else if (msg instanceof InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) {
            spec.setName(buildVolumeName(volume.getUuid()));
            createRootVolume((InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg) msg, spec);
        } else if (msg instanceof InstantiateTemporaryVolumeOnPrimaryStorageMsg) {
            spec.setName(buildReimageVolumeName(((InstantiateTemporaryVolumeOnPrimaryStorageMsg) msg).getOriginVolumeUuid()));
            createEmptyVolume(msg, spec);
        } else if (msg instanceof InstantiateMemoryVolumeOnPrimaryStorageMsg) {
            throw new UnsupportedOperationException();
        } else {
            spec.setName(buildVolumeName(volume.getUuid()));
            createEmptyVolume(msg, spec);
        }
    }

    protected void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
        String snapPath = msg.getSnapshot().getPrimaryStorageInstallPath();

        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setUuid(msg.getVolumeUuid());
        spec.setName(buildVolumeName(msg.getVolumeUuid()));

        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume == null) {
            reply.setError(operr("cannot find target volume[uuid:%s] to pass encryption attribute", msg.getVolumeUuid()));
            bus.reply(msg, reply);
            return;
        }
        spec.setSize(volume.getSize() != 0 ? volume.getSize() : msg.getSnapshot().getSize());
        spec.setEncrypted(volume.isEncrypted());
        ReturnValueCompletion<VolumeStats> completion = new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
                reply.setInstallPath(stats.getInstallPath());
                reply.setProtocol(externalVO.getDefaultProtocol());
                reply.setIncremental(stats.getParentUri() != null
                        && !controller.reportCapabilities().getSnapshotCapability().isSupportLazyDelete());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        };

        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE::isMatch)) {
            controller.cloneVolume(snapPath, spec, completion);
        } else {
            controller.copyVolume(snapPath, spec, completion);
        }
    }

    @Override
    protected void check(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
    }
    @Override
    protected void check(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
    }

    private void createRootVolume(InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg msg, CreateVolumeSpec spec) {
        final VmInstanceSpec.ImageSpec ispec = msg.getTemplateSpec();
        final ImageInventory image = ispec.getInventory();

        if (!ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(image.getMediaType())) {
            createEmptyVolume(msg, spec);
            return;
        }

        final VolumeInventory volume = msg.getVolume();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("external-create-root-volume-from-image-%s", image.getUuid()));
        chain.then(new ShareFlow() {
            String pathInCache;
            String installPath;
            String format;
            Long size;
            Long actualSize;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        downloadImageCache(msg.getTemplateSpec().getInventory(), new ReturnValueCompletion<ImageCacheInventory>(trigger) {
                            @Override
                            public void success(ImageCacheInventory returnValue) {
                                pathInCache = ImageCacheUtil.getImageCachePath(returnValue);
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
                    String __name__ = "create-template-from-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        controller.cloneVolume(pathInCache, spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats returnValue) {
                                size = returnValue.getSize();
                                actualSize = returnValue.getActualSize();
                                installPath = returnValue.getInstallPath();
                                format = returnValue.getFormat();
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
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        volume.setInstallPath(installPath);
                        volume.setSize(size);
                        volume.setActualSize(actualSize);
                        volume.setFormat(format);
                        if (StringUtils.isEmpty(volume.getProtocol())) {
                            volume.setProtocol(externalVO.getDefaultProtocol());
                        }
                        reply.setVolume(volume);
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

    private void createEmptyVolume(InstantiateVolumeOnPrimaryStorageMsg msg, CreateVolumeSpec spec) {
        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
        VolumeInventory volume = msg.getVolume();

        controller.createVolume(spec, new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                volume.setActualSize(stats.getActualSize());
                volume.setSize(stats.getSize());
                volume.setFormat(stats.getFormat());
                volume.setInstallPath(stats.getInstallPath());
                if (StringUtils.isEmpty(volume.getProtocol())) {
                    volume.setProtocol(externalVO.getDefaultProtocol());
                }
                reply.setVolume(volume);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(EncryptVolumeBitsOnPrimaryStorageMsg msg) {
        EncryptVolumeBitsOnPrimaryStorageReply reply = new EncryptVolumeBitsOnPrimaryStorageReply();
        controller.encryptVolumeBits(msg, new ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(EncryptVolumeBitsOnPrimaryStorageReply returnValue) {
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
    protected void handle(ConvertVolumeEncryptionOnPrimaryStorageMsg msg) {
        ConvertVolumeEncryptionOnPrimaryStorageReply reply = new ConvertVolumeEncryptionOnPrimaryStorageReply();
        controller.convertVolumeEncryption(msg,
                new ReturnValueCompletion<ConvertVolumeEncryptionOnPrimaryStorageReply>(msg) {
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

    protected void handle(final SetTrashExpirationTimeMsg msg) {
        controller.setTrashExpireTime(msg.getExpirationTime(), new Completion(msg) {
            @Override
            public void success() {
                SetTrashExpirationTimeReply reply = new SetTrashExpirationTimeReply();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SetTrashExpirationTimeReply reply = new SetTrashExpirationTimeReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final SelectBackupStorageMsg msg) {
        SelectBackupStorageReply reply = new SelectBackupStorageReply();


        BackupStorageSelector selector = pluginRgty.getExtensionFromMap(externalVO.getIdentity(), BackupStorageSelector.class);
        List<String> preferBsTypes = selector.getPreferBackupStorageTypes();
        if (!CollectionUtils.isEmpty(msg.getRequiredBackupStorageTypes())) {
            preferBsTypes.retainAll(msg.getRequiredBackupStorageTypes());
        }

        if (CollectionUtils.isEmpty(preferBsTypes)) {
            reply.setError(operr("no backup storage type specified support to primary storage[uuid:%s]", self.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        List<BackupStorageVO> availableBs = SQL.New("select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref" +
                " where bs.uuid = ref.backupStorageUuid" +
                " and ref.zoneUuid = :zoneUuid" +
                " and bs.status = :status" +
                " and bs.state = :state" +
                " and bs.availableCapacity > :size", BackupStorageVO.class)
                .param("zoneUuid", self.getZoneUuid())
                .param("status", BackupStorageStatus.Connected)
                .param("state", BackupStorageState.Enabled)
                .param("size", msg.getRequiredSize())
                .list();

        // sort by prefer type
        availableBs.sort(Comparator.comparingInt(o -> preferBsTypes.indexOf(o.getType())));
        reply.setInventory(BackupStorageInventory.valueOf(availableBs.get(0)));

        bus.reply(msg, reply);
    }

    private void handle(SetVolumeQosOnPrimaryStorageMsg msg) {
        SetVolumeQosOnPrimaryStorageReply reply = new SetVolumeQosOnPrimaryStorageReply();

        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        BaseVolumeInfo v = BaseVolumeInfo.valueOf(vol);

        VolumeQos qos = new VolumeQos();
        qos.setTotalBandwidth(msg.getTotalBandWidth());
        qos.setReadBandwidth(msg.getReadBandwidth());
        qos.setWriteBandwidth(msg.getWriteBandwidth());
        qos.setTotalIOPS(msg.getTotalIOPS());
        qos.setReadIOPS(msg.getReadIOPS());
        qos.setWriteIOPS(msg.getWriteIOPS());
        v.setQos(qos);
        controller.setVolumeQos(v, new Completion(msg) {
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

    private void handle(DeleteVolumeQosOnPrimaryStorageMsg msg) {
        DeleteVolumeQosOnPrimaryStorageReply reply = new DeleteVolumeQosOnPrimaryStorageReply();

        VolumeInventory vol = VolumeInventory.valueOf(dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class));
        BaseVolumeInfo v = BaseVolumeInfo.valueOf(vol);
        controller.deleteVolumeQos(v, new Completion(msg) {
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

    protected void handle(ResizeVolumeOnPrimaryStorageMsg msg) {
        controller.expandVolume(msg.getVolume(), msg.getSize(), new ReturnValueCompletion<VolumeStats>(msg) {
            final ResizeVolumeOnPrimaryStorageReply reply = new ResizeVolumeOnPrimaryStorageReply();

            @Override
            public void success(VolumeStats stats) {
                msg.getVolume().setSize(stats.getSize());
                reply.setVolume(msg.getVolume());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(TakeSnapshotMsg msg) {
        TakeSnapshotReply reply = new TakeSnapshotReply();
        VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class).eq(BlockVolumeVO_.uuid, sp.getVolumeUuid()).find();
        if (blockVolumeVO != null) {
            BlockExternalPrimaryStorageBackend backend = getBlockBackend(blockVolumeVO.getVendor());
            backend.handle(msg);
            return;
        }

        VolumeVO volumeVO = dbf.findByUuid(sp.getVolumeUuid(), VolumeVO.class);
        ZbsVolumeEncryptionExtensionPoint zbsEncryptionExtension = getZbsVolumeEncryptionExtension();
        if (zbsEncryptionExtension != null) {
            zbsEncryptionExtension.beforeTakeSnapshot(self.getUuid(), volumeVO, sp);
        }

        VolumeInventory vol = VolumeInventory.valueOf(volumeVO);
        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
        sspec.setVolumeInstallPath(vol.getInstallPath());
        sspec.setName(buildSnapshotName(sp.getUuid()));
        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(msg) {
            @Override
            public void success(VolumeSnapshotStats stats) {
                sp.setPrimaryStorageInstallPath(stats.getInstallPath());
                sp.setPrimaryStorageUuid(self.getUuid());
                sp.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                sp.setSize(stats.getActualSize());
                if (zbsEncryptionExtension != null) {
                    zbsEncryptionExtension.afterTakeSnapshot(self.getUuid(), volumeVO, sp);
                }
                reply.setInventory(sp);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private ZbsVolumeEncryptionExtensionPoint getZbsVolumeEncryptionExtension() {
        if (!ZbsVolumeEncryptionExtensionPoint.PRIMARY_STORAGE_IDENTITY.equals(controller.getIdentity())) {
            return null;
        }

        List<ZbsVolumeEncryptionExtensionPoint> exts = pluginRgty.getExtensionList(ZbsVolumeEncryptionExtensionPoint.class);
        if (exts.isEmpty()) {
            throw new OperationFailureException(operr("cannot find ZBS volume encryption extension"));
        }
        return exts.get(0);
    }

    private BlockExternalPrimaryStorageBackend getBlockBackend(String vendor) {
        BlockExternalPrimaryStorageFactory blockFactory = factory.blockExternalPrimaryStorageFactories.get(vendor);
        return blockFactory.getBlockExternalPrimaryStorageBackend(externalVO);
    }

    // TODO
    @Override
    protected void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-%s-on-primary-storage-%s", msg.getVolumeInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String imageCachePath;

            String snapshotPath;

            final String volumeUuid = msg.getVolumeInventory().getUuid();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "create-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(volumeUuid);

                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                        cmsg.setName("Snapshot-" + volumeUuid);
                        cmsg.setDescription("Take snapshot for " + volumeUuid);
                        cmsg.setVolumeUuid(volumeUuid);
                        cmsg.setAccountUuid(volumeAccountUuid);

                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply r) {
                                if (!r.isSuccess()) {
                                    trigger.fail(r.getError());
                                    return;
                                }

                                CreateVolumeSnapshotReply createVolumeSnapshotReply = (CreateVolumeSnapshotReply)r;
                                snapshotPath = createVolumeSnapshotReply.getInventory().getPrimaryStorageInstallPath();
                                trigger.next();
                            }
                        });

                    }
                });

                flow(new Flow() {
                    final String __name__ = "create-image-cache-from-volume-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildImageName(msg.getImageInventory().getUuid()));
                        spec.setUuid(msg.getVolumeInventory().getUuid());
                        spec.setSize(msg.getVolumeInventory().getSize());

                        ReturnValueCompletion<VolumeStats> completion = new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats dst) {
                                imageCachePath = dst.getInstallPath();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        };

                        if (msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat())) {
                            controller.cloneVolume(snapshotPath, spec, completion);
                        } else {
                            controller.copyVolume(snapshotPath, spec, completion);
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageCachePath == null) {
                            trigger.rollback();
                            return;
                        }
                        directDeleteBits(imageCachePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-cache-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
                        sspec.setVolumeInstallPath(imageCachePath);
                        sspec.setName(buildSnapshotName(msg.getImageInventory().getUuid()));
                        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(trigger) {
                            @Override
                            public void success(VolumeSnapshotStats returnValue) {
                                ImageCacheVO cvo = new ImageCacheVO();
                                cvo.setMd5sum("not calculated");
                                cvo.setInstallUrl(returnValue.getInstallPath());
                                cvo.setImageUuid((msg.getImageInventory().getUuid()));
                                cvo.setPrimaryStorageUuid(self.getUuid());
                                cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(
                                        msg.getImageInventory().getMediaType())
                                );
                                cvo.setState(ImageCacheState.ready);
                                cvo.setSize(returnValue.getActualSize());
                                dbf.persist(cvo);
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
                        CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                        // TODO get actual size
                        reply.setActualSize(msg.getVolumeInventory().getSize());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CreateImageCacheFromVolumeOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();

        boolean targetEncrypted = Boolean.TRUE.equals(msg.getEncrypted()) || Boolean.TRUE.equals(msg.getVolumeSnapshot().getEncrypted());
        boolean useSnapshotAsCache = msg.hasSystemTag(VolumeSystemTags.FAST_CREATE.getTagFormat())
                && (!targetEncrypted || Boolean.TRUE.equals(msg.getVolumeSnapshot().getEncrypted()));
        if (useSnapshotAsCache) {
            ImageCacheVO cache = createTemporaryImageCacheFromVolumeSnapshot(msg.getImageInventory(), msg.getVolumeSnapshot());
            dbf.persist(cache);
            reply.setInventory(cache.toInventory());

            boolean lazyDelete = controller.reportCapabilities().getSnapshotCapability().isSupportLazyDelete();
            /**
             If the primary storage supports lazy deletion, we can delete the snapshot immediately.
             Otherwise, we need to retain the snapshot until its references are deleted.
             The incremental clone mechanism handles this reference management.

             In other words, if the primary storage supports lazy deletion, we can treat it as a
             standard clone operation for simplified management.
             */
            reply.setIncremental(!lazyDelete);
            bus.reply(msg, reply);
            return;
        }

        createImageCacheFromSnapshot(msg);
    }

    private void createImageCacheFromSnapshot(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-snapshot-%s-on-primary-storage-%s", msg.getVolumeSnapshot().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            String imageCachePath;

            ImageCacheInventory inventory;

            ImageInventory image = msg.getImageInventory();

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "copy-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildImageName(image.getUuid()));
                        spec.setUuid(msg.getImageInventory().getUuid());
                        spec.setSize(msg.getImageInventory().getSize());
                        spec.setEncrypted(Boolean.TRUE.equals(msg.getEncrypted()) || Boolean.TRUE.equals(msg.getVolumeSnapshot().getEncrypted()));
                        controller.copyVolume(msg.getVolumeSnapshot().getPrimaryStorageInstallPath(), spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats dst) {
                                imageCachePath = dst.getInstallPath();
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (imageCachePath == null) {
                            trigger.rollback();
                            return;
                        }
                        directDeleteBits(imageCachePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new Flow() {
                    final String __name__ = "create-snapshot-for-clone";

                    boolean succ = false;

                    @Override
                    public boolean skip(Map data) {
                        return controller.reportCapabilities().isSupportCloneFromVolume();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotSpec spec = new CreateVolumeSnapshotSpec();
                        spec.setVolumeInstallPath(imageCachePath);
                        spec.setName(buildSnapshotName(image.getUuid()));
                        controller.createSnapshot(spec, new ReturnValueCompletion<VolumeSnapshotStats>(trigger) {
                            @Override
                            public void success(VolumeSnapshotStats stats) {
                                imageCachePath = stats.getInstallPath();
                                succ = true;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!succ) {
                            trigger.rollback();
                            return;
                        }
                        controller.deleteSnapshot(imageCachePath, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    final String __name__ = "persist-image-cache-vo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageCacheVO cache = new ImageCacheVO();
                        if (image.getMd5Sum() == null) {
                            cache.setMd5sum("not calculated");
                        } else {
                            cache.setMd5sum(image.getMd5Sum());
                        }
                        cache.setImageUuid(image.getUuid());
                        cache.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
                        cache.setInstallUrl(imageCachePath);
                        // TODO get size
                        cache.setSize(image.getSize());
                        cache.setPrimaryStorageUuid(self.getUuid());
                        dbf.persist(cache);
                        inventory = cache.toInventory();
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();
                        reply.setInventory(inventory);
                        reply.setIncremental(false);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-image-cache-from-volume-%s-on-primary-storage-%s", msg.getVolumeInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            RemoteTarget remoteTarget;
            ExportSpec espec = new ExportSpec();
            final String exportProtocol = controller.reportCapabilities().getDefaultImageExportProtocol() != null
                    ? controller.reportCapabilities().getDefaultImageExportProtocol().toString()
                    : rcf.getResourceConfigValue(ExternalPrimaryStorageGlobalConfig.IMAGE_EXPORT_PROTOCOL, self.getUuid(), String.class);

            String snapshotPath;
            String bsInstallPath;

            long templateSize;

            VolumeSnapshotInventory snapshot;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
                            snapshotPath = Q.New(VolumeSnapshotVO.class)
                                    .eq(VolumeSnapshotVO_.uuid, ((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg).getSnapshotUuid())
                                    .select(VolumeSnapshotVO_.primaryStorageInstallPath)
                                    .findValue();
                            return true;
                        }

                        // TODO vm offline
                        return false;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();

                        String volumeAccountUuid = acntMgr.getOwnerAccountUuidOfResource(msg.getVolumeInventory().getUuid());
                        cmsg.setName("snapshot-for-template-" + msg.getImageInventory().getUuid());
                        cmsg.setVolumeUuid(msg.getVolumeInventory().getUuid());
                        cmsg.setAccountUuid(volumeAccountUuid);
                        bus.makeLocalServiceId(cmsg, VolumeSnapshotConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    snapshot = ((CreateVolumeSnapshotReply) reply).getInventory();
                                    snapshotPath = snapshot.getPrimaryStorageInstallPath();
                                    trigger.next();
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (snapshot == null || !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class)) {
                            trigger.rollback();
                            return;
                        }

                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "export-volume-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        espec = new ExportSpec();
                        espec.setInstallPath(snapshotPath);
                        espec.setClientMnIp(getBsMnIp(msg.getBackupStorageUuid()));
                        espec.setClientQualifiedName(getClientQualifiedName(msg.getBackupStorageUuid(), exportProtocol));
                        controller.export(espec, VolumeProtocol.valueOf(exportProtocol), new ReturnValueCompletion<RemoteTarget>(trigger) {
                            @Override
                            public void success(RemoteTarget returnValue) {
                                remoteTarget = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (espec == null) {
                            trigger.rollback();
                            return;
                        }

                        controller.unexport(espec, remoteTarget, VolumeProtocol.valueOf(exportProtocol), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "import-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DownloadImageFromRemoteTargetMsg umsg = new DownloadImageFromRemoteTargetMsg();
                        umsg.setBackupStorageUuid(msg.getBackupStorageUuid());
                        umsg.setImage(msg.getImageInventory());
                        umsg.setRemoteTargetUrl(remoteTarget.getResourceURI());
                        bus.makeTargetServiceIdByResourceUuid(umsg, BackupStorageConstant.SERVICE_ID, msg.getBackupStorageUuid());
                        bus.send(umsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    DownloadImageFromRemoteTargetReply r = reply.castReply();
                                    bsInstallPath = r.getInstallPath();
                                    templateSize = r.getSize();
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unexport-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.unexport(espec, remoteTarget, VolumeProtocol.valueOf(exportProtocol), new Completion(trigger) {
                            @Override
                            public void success() {
                                // prevent to rollback again.
                                espec = null;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-snapshot";

                    @Override
                    public boolean skip(Map data) {
                        return snapshot == null || !PrimaryStorageGlobalConfig.UNDO_TEMP_SNAPSHOT.value(Boolean.class);
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VolumeSnapshotDeletionMsg dmsg = new VolumeSnapshotDeletionMsg();
                        dmsg.setTreeUuid(snapshot.getTreeUuid());
                        dmsg.setVolumeUuid(snapshot.getVolumeUuid());
                        dmsg.setSnapshotUuid(snapshot.getUuid());
                        dmsg.setScope(DeleteVolumeSnapshotScope.Single.toString());
                        dmsg.setDirection(DeleteVolumeSnapshotDirection.Commit.toString());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VolumeSnapshotConstant.SERVICE_ID, snapshot.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(msg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete snapshot [uuid:%s] after successfully " +
                                                    "creating image for volume [uuid:%s], because %s",
                                            snapshot.getUuid(), snapshot.getVolumeUuid(), reply.getError().getDetails()));
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        reply.setTemplateBackupStorageInstallPath(bsInstallPath);
                        reply.setFormat(msg.getVolumeInventory().getFormat());
                        reply.setActualSize(templateSize);
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

    // TODO, hardcode
    private String getClientQualifiedName(String bsUuid, String protocol) {
        if (VolumeProtocol.iSCSI.name().equals(protocol)) {
            return BackupStorageSystemTags.ISCSI_INITIATOR_NAME.getTokenByResourceUuid(bsUuid, BackupStorageVO.class, BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN);
        }

        return null;
    }

    private String getBsMnIp(String bsUuid) {
        GetBackupStorageManagerHostnameMsg msg = new GetBackupStorageManagerHostnameMsg();
        msg.setUuid(bsUuid);
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }
        return ((GetBackupStorageManagerHostnameReply) reply).getHostname();
    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setAllocatedUrl(msg.getAllocatedInstallUrl());
        spec.setName(buildVolumeName(msg.getVolumeUuid()));
        spec.setUuid(msg.getVolumeUuid());
        downloadImageTo(msg.getImage(), spec, VolumeVO.class.getSimpleName(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats returnValue) {
                reply.setInstallPath(returnValue.getInstallPath());
                reply.setFormat(msg.getImage().getFormat());
                reply.setProtocol(externalVO.getDefaultProtocol());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void downloadImageCache(ImageInventory image, ReturnValueCompletion<ImageCacheInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("download-image-cache-%s-to-%s", image.getUuid(), self.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDownloadImageCache(image, new ReturnValueCompletion<ImageCacheInventory>(chain) {
                    @Override
                    public void success(ImageCacheInventory returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
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

    private void doDownloadImageCache(ImageInventory image, ReturnValueCompletion<ImageCacheInventory> completion) {
        CreateVolumeSpec spec = new CreateVolumeSpec();
        spec.setUuid(image.getUuid());
        spec.setName(buildImageName(image.getUuid()));

        ImageCacheVO cache = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                .eq(ImageCacheVO_.imageUuid, image.getUuid())
                .find();
        if (cache != null) {
            // TODO check exists in ps
            completion.success(ImageCacheInventory.valueOf(cache));
            return;
        }

        downloadImageTo(image, spec, ImageCacheVO.class.getSimpleName(), new ReturnValueCompletion<VolumeStats>(completion) {
            @Override
            public void success(VolumeStats volStats) {
                ImageCacheVO cache = new ImageCacheVO();
                if (image.getMd5Sum() == null) {
                    cache.setMd5sum("not calculated");
                } else {
                    cache.setMd5sum(image.getMd5Sum());
                }
                cache.setImageUuid(image.getUuid());
                cache.setMediaType(ImageConstant.ImageMediaType.valueOf(image.getMediaType()));
                cache.setInstallUrl(volStats.getInstallPath());
                cache.setSize(volStats.getSize());
                cache.setPrimaryStorageUuid(self.getUuid());
                dbf.persist(cache);
                completion.success(ImageCacheInventory.valueOf(cache));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void downloadImageTo(ImageInventory image, CreateVolumeSpec spec, String targetClz, ReturnValueCompletion<VolumeStats> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-to-%s", image.getUuid(), spec.getName()));
        chain.then(new ShareFlow() {
            RemoteTarget remoteTarget;
            ExportSpec espec;
            VolumeStats volume;

            final String bsUuid = image.getBackupStorageRefs().get(0).getBackupStorageUuid();
            final String exportProtocol = controller.reportCapabilities().getDefaultImageExportProtocol() != null
                    ? controller.reportCapabilities().getDefaultImageExportProtocol().toString()
                    : rcf.getResourceConfigValue(ExternalPrimaryStorageGlobalConfig.IMAGE_EXPORT_PROTOCOL, self.getUuid(), String.class);
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "create-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        spec.setSize(Long.max(image.getActualSize(), image.getSize()));
                        controller.createVolume(spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats stats) {
                                volume = stats;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (volume == null) {
                            trigger.rollback();
                            return;
                        }
                        directDeleteBits(volume.getInstallPath(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = "export-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        espec = new ExportSpec();
                        espec.setInstallPath(volume.getInstallPath());
                        espec.setClientMnIp(getBsMnIp(bsUuid));
                        espec.setClientQualifiedName(getClientQualifiedName(bsUuid, exportProtocol));
                        controller.export(espec, VolumeProtocol.valueOf(exportProtocol), new ReturnValueCompletion<RemoteTarget>(trigger) {
                            @Override
                            public void success(RemoteTarget returnValue) {
                                remoteTarget = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (espec == null) {
                            trigger.rollback();
                            return;
                        }

                        controller.unexport(espec, remoteTarget, VolumeProtocol.valueOf(exportProtocol), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });

                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        UploadImageToRemoteTargetMsg dmsg = new UploadImageToRemoteTargetMsg();
                        dmsg.setBackupStorageUuid(bsUuid);
                        dmsg.setImage(image);
                        dmsg.setRemoteTargetUrl(remoteTarget.getResourceURI());
                        // TODO hardcode
                        dmsg.setFormat(controller.reportCapabilities().getSupportedImageFormats().get(0));
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                } else {
                                    trigger.next();
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "unexport-volume";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.unexport(espec, remoteTarget, VolumeProtocol.valueOf(exportProtocol), new Completion(trigger) {
                            @Override
                            public void success() {
                                // prevent to rollback again.
                                espec = null;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create-snapshot-if-needed";

                    @Override
                    public boolean skip(Map data) {
                        return !targetClz.equals(ImageCacheVO.class.getSimpleName()) || controller.reportCapabilities().isSupportCloneFromVolume();
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateVolumeSnapshotSpec sspec = new CreateVolumeSnapshotSpec();
                        sspec.setVolumeInstallPath(volume.getInstallPath());
                        sspec.setName(buildSnapshotName(spec.getUuid()));
                        controller.createSnapshot(sspec, new ReturnValueCompletion<VolumeSnapshotStats>(trigger) {
                            @Override
                            public void success(VolumeSnapshotStats returnValue) {
                                volume.setInstallPath(returnValue.getInstallPath());
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(volume);
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

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();

        AllocateSpaceSpec aspec = new AllocateSpaceSpec();
        aspec.setSize(msg.getImage().getSize());
        aspec.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume);

        String installPath = controller.allocateSpace(aspec);
        reply.setInstallPath(installPath);
        bus.reply(msg, reply);
    }

    protected void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg) {
        GetVolumeBackingChainFromPrimaryStorageReply r = new GetVolumeBackingChainFromPrimaryStorageReply();
        new While<>(msg.getRootInstallPaths()).each((installPath, compl) -> {
            getParentChain(installPath, new ArrayList<>(), new ReturnValueCompletion<List<VolumeStats>>(compl) {
                @Override
                public void success(List<VolumeStats> chains) {
                    long tsize = 0;
                    List<String> chainPaths = new ArrayList<>();
                    for (VolumeStats stats : chains) {
                        tsize += stats.getActualSize();
                        chainPaths.add(stats.getInstallPath());
                    }

                    r.putBackingChainInstallPath(installPath, chainPaths);
                    r.putBackingChainSize(installPath, tsize);
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.allDone();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    r.setError(errorCodeList.getCauses().get(0));
                }
                bus.reply(msg, r);
            }
        });
    }

    private void getParentChain(String installPath, List<VolumeStats> chains, ReturnValueCompletion<List<VolumeStats>> completion) {
        controller.stats(installPath, new ReturnValueCompletion<VolumeStats>(completion) {
            @Override
            public void success(VolumeStats stats) {
                chains.add(stats);
                if (stats.getParentUri() == null) {
                    chains.remove(0);  // do not contain self
                    completion.success(chains);
                    return;
                }

                getParentChain(stats.getParentUri(), chains, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(DeleteVolumeChainOnPrimaryStorageMsg msg) {
        DeleteVolumeChainOnPrimaryStorageReply reply = new DeleteVolumeChainOnPrimaryStorageReply();
        new While<>(msg.getInstallPaths()).each((installPath, compl) -> {
            // TODO use trash instead, make sure only one snapshot for link clone.
            controller.deleteVolumeAndSnapshot(installPath, new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    compl.addError(errorCode);
                    compl.allDone();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    reply.setError(errorCodeList.getCauses().get(0));
                    bus.reply(msg, reply);
                } else {
                    bus.reply(msg, reply);
                }
            }
        });
    }

    @Override
    protected void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        String protocol = null;
        boolean force = false;
        if (VolumeVO.class.getSimpleName().equals(msg.getBitsType()) && msg.getBitsUuid() != null) {
            VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getBitsUuid()).find();
            if (volume != null) {
                protocol = volume.getProtocol();
            }

             // FIXME(shenjin) remove it
            if (volume != null && VolumeType.Root.equals(volume.getType()) && VolumeProtocol.iSCSI.toString().equals(protocol)) {
                force = true;
            }
        }

        trashVolume(msg.getInstallPath(), protocol, force, new Completion(msg) {
            @Override
            public void success() {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
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
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getVolume().getUuid()).find();
        if (blockVolumeVO != null) {
            BlockExternalPrimaryStorageBackend backend = getBlockBackend(blockVolumeVO.getVendor());
            backend.handle(msg);
            return;
        }
        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
        boolean force = VolumeType.Root.toString().equals(msg.getVolume().getType()) &&
                VolumeProtocol.iSCSI.toString().equals(msg.getVolume().getProtocol());
        trashVolume(msg.getVolume().getInstallPath(), msg.getVolume().getProtocol(), force, new Completion(msg) {
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
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        // only consider volume bits because Deprecated, will be removed in future
        trashVolume(msg.getInstallPath(), null, false, new Completion(msg) {
            @Override
            public void success() {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();
        // TODO deactivate first
        controller.deleteVolumeAndSnapshot(msg.getInstallPath(), new Completion(msg) {
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
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {
        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
        downloadImageCache(msg.getIsoSpec().getInventory(), new ReturnValueCompletion<ImageCacheInventory>(msg) {
            @Override
            public void success(ImageCacheInventory cache) {
                String isoProtocol = controller.reportCapabilities().getDefaultIsoActiveProtocol() != null
                        ? controller.reportCapabilities().getDefaultIsoActiveProtocol().toString()
                        : ExternalPrimaryStorageGlobalConfig.IMAGE_EXPORT_PROTOCOL.value(String.class);

                HostInventory host = HostInventory.valueOf(dbf.findByUuid(msg.getDestHostUuid(), HostVO.class));
                node.activate(BaseVolumeInfo.valueOf(cache, isoProtocol), host, true, new ReturnValueCompletion<ActiveVolumeTO>(msg) {
                    @Override
                    public void success(ActiveVolumeTO returnValue) {
                        reply.setInstallPath(cache.getInstallUrl());
                        reply.setProtocol(isoProtocol);
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
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {
        // The ISO is in the image cache, no need to delete it
        bus.reply(msg, new DeleteIsoFromPrimaryStorageReply());
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        reply.setCapability(controller.reportCapabilities().getSnapshotCapability());
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(SyncVolumeSizeOnPrimaryStorageMsg msg) {
        SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
        controller.stats(msg.getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
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
    protected void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg) {
        EstimateVolumeTemplateSizeOnPrimaryStorageReply reply = new EstimateVolumeTemplateSizeOnPrimaryStorageReply();
        controller.stats(msg.getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
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
    protected void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg) {
        BatchSyncVolumeSizeOnPrimaryStorageReply reply = new BatchSyncVolumeSizeOnPrimaryStorageReply();

        Map<String, String> installPathToUuids = msg.getVolumeUuidInstallPaths().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getValue, Map.Entry::getKey, (k1, k2) -> k1
        ));
        controller.batchStats(msg.getVolumeUuidInstallPaths().values(), new ReturnValueCompletion<List<VolumeStats>>(msg) {
            @Override
            public void success(List<VolumeStats> stats) {
                Map<String, Long> actualSizeByUuids = stats.stream().collect(Collectors.toMap(
                        s -> installPathToUuids.get(s.getInstallPath()), VolumeStats::getActualSize, (k1, k2) -> k1
                ));
                reply.setActualSizes(actualSizeByUuids);
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
    protected void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
        controller.deleteSnapshot(msg.getFrom().getPrimaryStorageInstallPath(), new Completion(msg) {
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
        FlattenVolumeOnPrimaryStorageReply reply = new FlattenVolumeOnPrimaryStorageReply();
        controller.flattenVolume(msg.getVolume().getInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setActualSize(stats.getActualSize());
                reply.setSize(stats.getSize());
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
    protected void handle(DeleteSnapshotOnPrimaryStorageMsg msg) {
        BlockVolumeVO blockVolumeVO = Q.New(BlockVolumeVO.class)
                .eq(BlockVolumeVO_.uuid, msg.getSnapshot().getVolumeUuid()).find();
        if (blockVolumeVO != null) {
            BlockExternalPrimaryStorageBackend backend = getBlockBackend(blockVolumeVO.getVendor());
            backend.handle(msg);
            return;
        }
        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
        controller.deleteSnapshot(msg.getSnapshot().getPrimaryStorageInstallPath(), new Completion(msg) {
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
    protected void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
        controller.revertVolumeSnapshot(msg.getSnapshot().getPrimaryStorageInstallPath(), new ReturnValueCompletion<VolumeStats>(msg) {
            @Override
            public void success(VolumeStats stats) {
                reply.setNewVolumeInstallPath(stats.getInstallPath());
                reply.setSize(stats.getSize());
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
    protected void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("reimage-vm-root-volume-%s", msg.getVolume().getUuid()));
        chain.then(new ShareFlow() {
            String volumePath;
            String installUrl;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "download-image-to-cache";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        String imageCacheInstallUrl = Q.New(ImageCacheVO.class)
                                .eq(ImageCacheVO_.primaryStorageUuid, self.getUuid())
                                .eq(ImageCacheVO_.imageUuid, msg.getVolume().getRootImageUuid())
                                .select(ImageCacheVO_.installUrl).findValue();

                        if (imageCacheInstallUrl != null) {
                            installUrl = imageCacheInstallUrl;
                            trigger.next();
                            return;
                        }

                        ImageVO image = dbf.findByUuid(msg.getVolume().getRootImageUuid(), ImageVO.class);
                        if (image == null) {
                            trigger.fail(operr("root image and root image cache has been deleted, cannot reimage now"));
                            return;
                        }

                        downloadImageCache(ImageInventory.valueOf(image), new ReturnValueCompletion<ImageCacheInventory>(trigger) {
                            @Override
                            public void success(ImageCacheInventory returnValue) {
                                installUrl = returnValue.getInstallUrl();
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
                    String __name__ = "clone-image";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        CreateVolumeSpec spec = new CreateVolumeSpec();
                        spec.setName(buildReimageVolumeName(msg.getVolume().getUuid()));
                        spec.setUuid(msg.getVolume().getUuid());
                        spec.setSize(msg.getVolume().getSize());
                        spec.setEncrypted(Boolean.TRUE.equals(msg.getVolume().getEncrypted()));
                        controller.cloneVolume(installUrl, spec, new ReturnValueCompletion<VolumeStats>(trigger) {
                            @Override
                            public void success(VolumeStats returnValue) {
                                volumePath = returnValue.getInstallPath();
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
                        reply.setNewVolumeInstallPath(volumePath);
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
    protected void handle(AskInstallPathForNewSnapshotMsg msg) {
        AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(GetPrimaryStorageResourceLocationMsg msg) {
        bus.reply(msg, new GetPrimaryStorageResourceLocationReply());
    }

    @Override
    protected void handle(CheckVolumeSnapshotOperationOnPrimaryStorageMsg msg) {
        bus.reply(msg, new CheckVolumeSnapshotOperationOnPrimaryStorageReply());
    }

    @Override
    protected void connectHook(ConnectParam param, Completion completion) {
        controller.validateConfig(externalVO.getConfig());
        controller.connect(externalVO.getConfig(), self.getUrl(), new ReturnValueCompletion<LinkedHashMap>(completion) {
            @Override
            public void success(LinkedHashMap addonInfo) {
                if (param.isNewAdded()) {
                    controller.onFirstAdditionConfigure(new NopeCompletion());
                }
                SQL.New(ExternalPrimaryStorageVO.class).eq(ExternalPrimaryStorageVO_.uuid, self.getUuid())
                        .set(ExternalPrimaryStorageVO_.addonInfo, JSONObjectUtil.toJsonString(addonInfo))
                        .update();
                controller.setTrashExpireTime(PrimaryStorageGlobalConfig.TRASH_EXPIRATION_TIME.value(Integer.class), new NopeCompletion());
                pingHook(completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void pingHook(Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("ping-external-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    final String __name__ = "ping-storage";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.ping(new Completion(trigger) {
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

                flow(new NoRollbackFlow() {
                    final String __name__ = "report-capacity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        controller.reportCapacity(new ReturnValueCompletion<StorageCapacity>(trigger) {
                            @Override
                            public void success(StorageCapacity capacity) {
                                if (capacity.getHealthy() == StorageHealthy.Ok || capacity.getHealthy() == StorageHealthy.Warn) {
                                    new PrimaryStorageCapacityUpdater(self.getUuid()).run(cap -> {
                                        if (cap.getTotalCapacity() == 0 || cap.getAvailableCapacity() == 0) {
                                            cap.setAvailableCapacity(capacity.getAvailableCapacity());
                                        }

                                        cap.setTotalCapacity(capacity.getTotalCapacity());
                                        cap.setTotalPhysicalCapacity(capacity.getTotalCapacity());
                                        cap.setAvailablePhysicalCapacity(capacity.getAvailableCapacity());

                                        return cap;
                                    });
                                    trigger.next();
                                } else {
                                    trigger.fail(operr("storage is not healthy:%s", capacity.getHealthy().toString()));
                                }
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
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

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        controller.reportCapacity(new ReturnValueCompletion<StorageCapacity>(completion) {
            @Override
            public void success(StorageCapacity usage) {
                PhysicalCapacityUsage ret = new PhysicalCapacityUsage();
                ret.totalPhysicalSize = usage.getTotalCapacity();
                ret.availablePhysicalSize = usage.getAvailableCapacity();
                completion.success(ret);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    protected void handle(ShrinkVolumeSnapshotOnPrimaryStorageMsg msg) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg) {
        throw new UnsupportedOperationException();
    }

    private void directDeleteBits(String installPath, Completion completion) {
        controller.deleteVolume(installPath, completion);
    }

    protected void trashVolume(String installPath, String protocol, boolean force, Completion completion) {
        deactivateAndDeleteVolume(installPath, protocol, force, completion);
    }

    protected void deactivateAndDeleteVolume(String installPath, String protocol, boolean force, Completion completion) {
        if (protocol == null) {
            doDeleteBits(installPath, force, completion);
            return;
        }

        List<ActiveVolumeClient> clients = node.getActiveClients(installPath, protocol);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("deactivate-and-delete-volume-%s", installPath));
        chain.then(new NoRollbackFlow() {
            final String __name__ = "deactivate-volume";

            @Override
            public boolean skip(Map data) {
                return clients.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(clients).each((client, c) -> {
                    node.deactivate(installPath, protocol, client, new Completion(c) {
                        @Override
                        public void success() {
                            c.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            c.addError(errorCode);
                            c.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            final String __name__ = "delete-volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                doDeleteBits(installPath, force, new Completion(trigger) {
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

    private void doDeleteBits(String installPath, boolean force, Completion completion) {
        if (force) {
            controller.deleteVolume(installPath, completion);
        } else {
            controller.trashVolume(installPath, completion);
        }
    }

    @Override
    protected void doAddProtocol(APIAddStorageProtocolMsg msg, Completion completion) {
        ExternalPrimaryStorageVO storageVO = Q.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, msg.getUuid())
                .find();
        if (storageVO != null) {
            PrimaryStorageOutputProtocolRefVO ref = Q.New(PrimaryStorageOutputProtocolRefVO.class)
                    .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, msg.getUuid())
                    .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, msg.getOutputProtocol())
                    .find();
            storageVO.getOutputProtocols().add(ref);
            dbf.updateAndRefresh(storageVO);
        }
        super.doAddProtocol(msg, completion);
    }
}
