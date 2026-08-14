package org.zstack.storage.primary.block;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.compute.vm.IsoOperator;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.RunInQueue;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.DocUtils;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathMsg;
import org.zstack.header.storage.backup.BackupStorageAskInstallPathReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.DeleteBitsOnBackupStorageMsg;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.kvm.*;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.primary.block.message.APIAddBlockPrimaryStorageMsg;
import org.zstack.storage.primary.block.message.CancelSelfFencerOnKvmHostMsg;
import org.zstack.storage.primary.block.message.InitBlockPrimaryStorageOnHostConnectedMsg;
import org.zstack.storage.primary.block.message.SetupSelfFencerOnKvmHostMsg;
import org.zstack.storage.snapshot.MarkRootVolumeAsSnapshotExtension;
import org.zstack.storage.volume.ChangeVolumeInstallPathExtensionPoint;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/2 16:15
 */
public class BlockPrimaryStorageFactory implements PrimaryStorageFactory, PrimaryStorageAttachExtensionPoint,
        PrimaryStorageDetachExtensionPoint, BackupStorageBlockKvmFactory, Component, KVMStartVmExtensionPoint,
        KVMHostConnectExtensionPoint, VmInstanceMigrateExtensionPoint, ResizeVolumeExtensionPoint,
        RecalculatePrimaryStorageCapacityExtensionPoint, ChangeVolumeInstallPathExtensionPoint,
        OverwriteVolumeExtensionPoint, KVMAttachVolumeExtensionPoint, KVMDetachVolumeExtensionPoint,
        BeforeTakeLiveSnapshotsOnVolumes, CreateTemplateFromVolumeSnapshotExtensionPoint, MarkRootVolumeAsSnapshotExtension,
        PreVmInstantiateResourceExtensionPoint, KvmSetupSelfFencerExtensionPoint, VmPreMigrationExtensionPoint,
        KVMPingAgentNoFailureExtensionPoint, VmAttachVolumeExtensionPoint, KVMConvertVolumeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BlockPrimaryStorageFactory.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ThreadFacade thdf;

    public static final PrimaryStorageType type = new PrimaryStorageType(BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE);

    private static Map<String, BlockPrimaryStorageDeviceBackend> blockPrimaryStorageDeviceBackends = Collections.synchronizedMap(new HashMap<String, BlockPrimaryStorageDeviceBackend>());

    static {
        type.setSupportHeartbeatFile(true);
        type.setSupportVmLiveMigration(true);
        type.setSupportConfigVolumeProvisionStrategy(true);
    }

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private PluginRegistry pluginRegistry;


    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return type;
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new BlockPrimaryStorageBase(vo);
    }

    @Override
    public PrimaryStorageInventory getInventory(String uuid) {
        return PrimaryStorageInventory.valueOf(dbf.findByUuid(uuid, PrimaryStorageVO.class));
    }

    @Override
    public void validateStorageProtocol(String protocol) {

    }

    @Override
    @Transactional
    public PrimaryStorageInventory createPrimaryStorage(PrimaryStorageVO vo, APIAddPrimaryStorageMsg addPrimaryStorageMsg) {

        APIAddBlockPrimaryStorageMsg msg = (APIAddBlockPrimaryStorageMsg) addPrimaryStorageMsg;

        BlockPrimaryStorageVO bpsvo = new BlockPrimaryStorageVO(vo);
        bpsvo.setType(type.toString());
        bpsvo.setUrl(String.format("%s%s", BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME, vo.getUuid()));
        bpsvo.setMountPath(String.format("%s%s", BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME, vo.getUuid()));
        bpsvo.setUuid(vo.getUuid());
        bpsvo.setMetadata(msg.getMetadata());
        bpsvo.setVendorName(msg.getVendorName());
        bpsvo.setState(PrimaryStorageState.Enabled);
        bpsvo.setStatus(PrimaryStorageStatus.Connecting);
        bpsvo.setZoneUuid(msg.getZoneUuid());
        bpsvo.setName(msg.getName());

        bpsvo = dbf.persistAndRefresh(bpsvo);
        return PrimaryStorageInventory.valueOf(bpsvo);
    }

    @Override
    public void preAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void beforeAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterAttachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (!inventory.getType().equals(BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE)) {
            return;
        }

        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
        rmsg.setPrimaryStorageUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, inventory.getUuid());
        bus.send(rmsg);
    }

    @Override
    public void preDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
    }

    @Override
    public void beforeDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void failToDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {

    }

    @Override
    public void afterDetachPrimaryStorage(PrimaryStorageInventory inventory, String clusterUuid) {
        if (!inventory.getType().equals(BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE)) {
            return;
        }

        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
        rmsg.setPrimaryStorageUuid(inventory.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, inventory.getUuid());
        bus.send(rmsg);
    }

    @Override
    public boolean start() {
        populateBlockStorageFactories();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateBlockStorageFactories() {
        logger.debug(String.format("start to populate block storage factories"));
        for (BlockPrimaryStorageDeviceBackend ext : pluginRegistry.getExtensionList(BlockPrimaryStorageDeviceBackend.class)) {
            BlockPrimaryStorageDeviceBackend old = blockPrimaryStorageDeviceBackends.get(ext.getVendorName());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate block primary storage device: %s", ext.getVendorName()));
            }
            blockPrimaryStorageDeviceBackends.put(ext.getVendorName(), ext);
        }
    }

    public static BlockPrimaryStorageDeviceBackend getBlockPrimaryStorageDeviceBackend(BlockPrimaryStorageVO bpsvo) {
        if (bpsvo == null) {
            return null;
        }
        String vendor = bpsvo.getVendorName();
        BlockPrimaryStorageDeviceBackend bpsdb = blockPrimaryStorageDeviceBackends.get(vendor);
        if (bpsdb == null) {
            throw new CloudRuntimeException(String.format("No block primary storage device found %s", vendor));
        }
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = bpsdb.getBlockPrimaryStorageDeviceBackend(bpsvo);
        return blockPrimaryStorageDeviceBackend;
    }

    public BlockPrimaryStorageDeviceBackend getBlockPrimaryStorageDeviceBackend(String primaryStorageUuid) {
        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, primaryStorageUuid)
                .find();
        if (blockPrimaryStorageVO == null) {
            return null;
        }
        return getBlockPrimaryStorageDeviceBackend((blockPrimaryStorageVO));
    }

    public BlockPrimaryStorageDeviceBackend getBlockPrimaryStorageDeviceBackend(String vendorName, String metadata) {
        BlockPrimaryStorageVO bpsvo = new BlockPrimaryStorageVO();
        bpsvo.setVendorName(vendorName);
        bpsvo.setMetadata(metadata);
        return getBlockPrimaryStorageDeviceBackend(bpsvo);
    }

    @Override
    public String getBackupStorageType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }

    @Override
    public BackupStorageBlockKvmUploader createUploader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageBlockKvmUploader.createUploader(ps, bsUuid);
    }

    @Override
    public BackupStorageBlockKvmDownloader createDownloader(PrimaryStorageInventory ps, String bsUuid) {
        return ImageStoreBackupStorageBlockKvmDownloader.createDownloader(ps, bsUuid);
    }

    @Override
    public String getPrimaryStorageTypeForRecalculateCapacityExtensionPoint() {
        return type.toString();
    }

    @Override
    public void afterRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct) {

    }

    @Override
    public void beforeRecalculatePrimaryStorageCapacity(RecalculatePrimaryStorageCapacityStruct struct) {

    }

    private VolumeTO convertVolumeToBlockIfNeeded(VolumeInventory vol, VolumeTO to) {
        if (!vol.getInstallPath().startsWith(VolumeTO.BLOCK)) {
            return to;
        }

        to.setInstallPath(to.getInstallPath().replace(BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME, "/dev/disk/by-id/wwn-0x"));
        return to;
    }

    private String convertIsoToBlockIfNeeded(String isoPath) {
        if (!isoPath.startsWith(VolumeTO.BLOCK)) {
            return isoPath;
        }

        return isoPath.replace(BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME, "/dev/disk/by-id/wwn-0x");
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        cmd.setRootVolume(convertVolumeToBlockIfNeeded(spec.getDestRootVolume(), cmd.getRootVolume()));

        List<VolumeTO> dtos = new ArrayList<VolumeTO>();
        for (VolumeTO to : cmd.getDataVolumes()) {
            for (VolumeInventory vol : spec.getDestDataVolumes()) {
                if (vol.getUuid().equals(to.getVolumeUuid())) {
                    dtos.add(convertVolumeToBlockIfNeeded(vol, to));
                    break;
                }
            }
        }

        if(isBlockPrimaryStorage(spec.getDestRootVolume().getInstallPath(), spec.getDestRootVolume().getPrimaryStorageUuid())) {
            List<String> oemStrings = cmd.getOemStrings();
            oemStrings.add("storage:" + spec.getDestRootVolume().getPrimaryStorageUuid());
            cmd.setOemStrings(oemStrings);
        }

        cmd.setDataVolumes(dtos);

        for (KVMAgentCommands.CdRomTO cdRomTO : cmd.getCdRoms()) {
            if (cdRomTO.isEmpty()) {
                continue;
            }
            cdRomTO.setPath(convertIsoToBlockIfNeeded(cdRomTO.getPath()));
        }


    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Transactional(readOnly = true)
    private List<String> findBlockStorageUuidByHostUuid(String clusterUuid) {
        String sql = "select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("cuuid", clusterUuid);
        q.setParameter("ptype", BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE);
        if (q.getResultList() == null) {
            return Collections.EMPTY_LIST;
        }
        return q.getResultList();
    }

    @Override
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-block-storage";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                final List<String> prUuids = findBlockStorageUuidByHostUuid(context.getInventory().getClusterUuid());
                if (prUuids.isEmpty()) {
                    trigger.next();
                    return;
                }
                Iterator<String> iterator = prUuids.iterator();
                initBlockStorage(iterator, trigger, data, context);
            }
        };
    }

    private void initBlockStorage(final Iterator<String> iterator,
                                  final FlowTrigger trigger,
                                  Map data,
                                  final KVMHostConnectedContext context) {
        if (!iterator.hasNext()) {
            trigger.next();
            return;
        }

        final String priUuid = iterator.next();

        HostVO host = new HostVO();
        host.setUuid(context.getInventory().getUuid());
        saveBlockPrimaryStorageHostsRefIfNotExist(Collections.singletonList(host), priUuid);
        InitBlockPrimaryStorageOnHostConnectedMsg msg = new InitBlockPrimaryStorageOnHostConnectedMsg();
        msg.setPrimaryStorageUuid(priUuid);
        msg.setHostUuid(context.getInventory().getUuid());
        msg.setNewAdded(context.isNewAddedHost());
        bus.makeTargetServiceIdByResourceUuid(msg, BlockPrimaryStorageConstants.SERVICE_ID, priUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(operr("KVM host[uuid: %s] fails to be added into block primary storage[uuid: %s], %s",
                            context.getInventory().getUuid(), priUuid, reply.getError()));
                } else {
                    initBlockStorage(iterator, trigger, data, context);
                }
            }
        });

    }

    private static boolean isBlockPrimaryStorage(String installPath, String psUuid) {
        if (installPath != null && !installPath.equals("") && installPath.startsWith(BlockPrimaryStorageConstants.BLOCK_INSTALL_PATH_SCHEME)) {
            return true;
        }

        if ((installPath == null || installPath.equals("") ) && psUuid != null) {
            return Q.New(BlockPrimaryStorageVO.class)
                    .eq(BlockPrimaryStorageVO_.uuid, psUuid)
                    .isExists();
        }
        return false;
    }

    private void mapIsoLunPreMigrate(VmInstanceInventory inv, String destHostUuid) {
        if(!isBlockPrimaryStorage(inv.getRootVolume().getInstallPath(), inv.getRootVolume().getPrimaryStorageUuid())) {
            return;
        }

        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(inv.getUuid());
        if (isoUuids == null || isoUuids.isEmpty()) {
            return;
        }
        FutureCompletion completion = new FutureCompletion(null);
        new While<>(isoUuids).each((String isoUuid, WhileCompletion whileCompletion) -> {
            String primaryStorageUuid = getVMRelatedIsoImageCachePrimaryStorageUuid(inv, isoUuid);

            if (primaryStorageUuid.length() == 0) {
                whileCompletion.addError(operr("iso[uuid: %s] is attached to vm[uuid: ], but iso is not on any block storage, you have to detach it, before migrate vm", isoUuid, inv.getUuid()));
                whileCompletion.done();
            } else {
                BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(primaryStorageUuid);
                String lunName = generateImageCacheLunName(isoUuid, primaryStorageUuid);

                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(destHostUuid, primaryStorageUuid);
                Map data = new HashMap();
                data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
                data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName("map iso to dest host before migrate");
                chain.setData(data);
                chain.then(new NoRollbackFlow() {
                    String __name__ = "get iso lun info";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bkd.getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                            @Override
                            public void success(BlockScsiLunVO returnValue) {
                                BlockScsiLunVO isoLun = new BlockScsiLunVO();
                                isoLun.setId(returnValue.getId());
                                isoLun.setWwn(returnValue.getWwn());
                                isoLun.setLunType(returnValue.getLunType());
                                isoLun.setName(returnValue.getName());
                                data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, isoLun);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                chain.then(new MapLunToHostFlow());
                chain.done(new FlowDoneHandler(whileCompletion) {
                    @Override
                    public void handle(Map data) {
                        whileCompletion.done();
                    }
                }).error(new FlowErrorHandler(whileCompletion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        whileCompletion.addError(errCode);
                        whileCompletion.done();
                    }
                }).start();
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void doUnmapIso(String lunName, BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, SyncTaskChain taskChain, WhileCompletion whileCompletion) {
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageHostRefVO.getPrimaryStorageUuid());
        BlockScsiLunVO isoLun = new BlockScsiLunVO();

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);
        data.put(BlockPrimaryStorageConstants.Params.CheckBeforeDeleteLunMap, true);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("unmap iso from source host after migrating");
        chain.setData(data);
        chain.then(new NoRollbackFlow() {
            String __name__ = "get iso lun info";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        isoLun.setId(returnValue.getId());
                        isoLun.setWwn(returnValue.getWwn());
                        isoLun.setLunType(returnValue.getLunType());
                        isoLun.setName(returnValue.getName());
                        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, isoLun);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        });
        chain.then(new DeleteLunMapFlow());
        chain.done(new FlowDoneHandler(whileCompletion) {
            @Override
            public void handle(Map data) {
                whileCompletion.done();
                taskChain.next();
            }
        }).error(new FlowErrorHandler(whileCompletion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                whileCompletion.addError(errCode);
                whileCompletion.done();
                taskChain.next();
            }
        }).start();
    }

    private void unMapIsoLunAfterMigrate(VmInstanceInventory inv, String originHostUuid) {
        if(!isBlockPrimaryStorage(inv.getRootVolume().getInstallPath(), inv.getRootVolume().getPrimaryStorageUuid())) {
            return;
        }

        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(inv.getUuid());
        if (isoUuids == null || isoUuids.isEmpty()) {
            return;
        }
        FutureCompletion completion = new FutureCompletion(null);
        new While<>(isoUuids).each((String isoUuid, WhileCompletion whileCompletion) -> {

            List<String> useSameIsoOnTheSameHost = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.uuid)
                    .in(VmInstanceVO_.uuid, IsoOperator.getVmUuidByIsoUuid(isoUuid))
                    .eq(VmInstanceVO_.hostUuid, originHostUuid)
                    .listValues();

            if (!useSameIsoOnTheSameHost.isEmpty()) {
                whileCompletion.done();
                return;
            }

            if (useSameIsoOnTheSameHost.size() == 1 && useSameIsoOnTheSameHost.get(0).equals(inv.getUuid())) {
                whileCompletion.done();
                return;
            }

            String primaryStorageUuid = getVMRelatedIsoImageCachePrimaryStorageUuid(inv, isoUuid);

            if (primaryStorageUuid.length() == 0) {
                whileCompletion.addError(operr("iso[uuid: %s] is attached to vm[uuid: ], but iso is not on any block storage, you have to detach it, before migrate vm", isoUuid, inv.getUuid()));
                whileCompletion.done();
            } else {
                String lunName = generateImageCacheLunName(isoUuid, primaryStorageUuid);

                BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(originHostUuid, primaryStorageUuid);
                lunMapOperationInQueue()
                        .name(isoUuid)
                        .asyncBackup(completion)
                        .run(chain -> doUnmapIso(lunName, blockPrimaryStorageHostRefVO, chain, whileCompletion));
            }
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {

        List<VolumeInventory> volumeInventories = inv.getAllVolumes().stream()
                .filter(vol -> isBlockPrimaryStorage(vol.getInstallPath(), vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (volumeInventories.isEmpty()) {
            return;
        }
        List<String> volumeUuids = volumeInventories.stream()
                .map(VolumeInventory::getUuid).collect(Collectors.toList());

        List<BlockScsiLunVO> blockScsiLunVOList = Q.New(BlockScsiLunVO.class)
                .in(BlockScsiLunVO_.volumeUuid, volumeUuids)
                .list();

        mapIsoLunPreMigrate(inv, destHostUuid);

        FutureCompletion completion = new FutureCompletion(null);

        new While<>(blockScsiLunVOList).step((blockScsiLunVO, whileCompletion) -> {
            VolumeInventory volumeInventory = volumeInventories.stream()
                    .filter(volumeInventory1 -> volumeInventory1.getUuid().equals(blockScsiLunVO.getVolumeUuid()))
                    .findFirst().get();
            BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                    .eq(BlockPrimaryStorageVO_.uuid, volumeInventory.getPrimaryStorageUuid())
                    .find();
            BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);
            BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(destHostUuid, volumeInventory.getPrimaryStorageUuid());

            Map data = new HashMap();
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName("pre-vm-migration-prepare-lun-on-new-host");
            chain.setData(data);
            chain.then(new MapLunToHostFlow());
            chain.done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.done();
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    whileCompletion.addError(errCode);
                    whileCompletion.allDone();
                }
            }).start();
        }, 1).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });

        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {

        List<VolumeInventory> volumeInventories = inv.getAllVolumes().stream()
                .filter(vol -> isBlockPrimaryStorage(vol.getInstallPath(), vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (volumeInventories.isEmpty()) {
            return;
        }
        List<String> volumeUuids = volumeInventories.stream()
                .map(VolumeInventory::getUuid).collect(Collectors.toList());

        List<BlockScsiLunVO> blockScsiLunVOList = Q.New(BlockScsiLunVO.class)
                .in(BlockScsiLunVO_.volumeUuid, volumeUuids)
                .list();

        unMapIsoLunAfterMigrate(inv, srcHostUuid);

        FutureCompletion completion = new FutureCompletion(null);

        new While<>(blockScsiLunVOList).step((blockScsiLunVO, whileCompletion) -> {
            VolumeInventory volumeInventory = volumeInventories.stream()
                    .filter(volumeInventory1 -> volumeInventory1.getUuid().equals(blockScsiLunVO.getVolumeUuid()))
                    .findFirst().get();
            BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                    .eq(BlockPrimaryStorageVO_.uuid, volumeInventory.getPrimaryStorageUuid())
                    .find();
            BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);
            BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(srcHostUuid, volumeInventory.getPrimaryStorageUuid());

            Map data = new HashMap();
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName("pre-vm-migration-prepare-lun-on-new-host");
            chain.setData(data);
            chain.then(new DeleteLunMapFlow());
            chain.done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.done();
                }
            }).error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    whileCompletion.addError(errCode);
                    whileCompletion.allDone();
                }
            }).start();
        }, 1).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });
    }

    @Override
    public void beforeResizeVolume(VolumeVO volume, long resize, VolumeType type, ResizeVolumeStruct struct, Completion completion) {
        if (volume == null) {
            completion.success();
            return;
        }
        BlockPrimaryStorageVO bpsVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, volume.getPrimaryStorageUuid())
                .find();

        if (bpsVO == null) {
            completion.success();
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(bpsVO);
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, volume.getUuid())
                .find();

        if (blockScsiLunVO == null) {
            completion.fail(operr("fail to find block scsi lun for volume: %s", volume.getUuid()));
            return;
        }

        blockScsiLunVO.setSize(resize);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("resize volume on block primary storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                bkd.resizeLun(blockScsiLunVO, resize, new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });

            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger flowTrigger, Map map) {
                String vmInstanceUuid = volume.getVmInstanceUuid();
                if (vmInstanceUuid == null || vmInstanceUuid.isEmpty()) {
                    flowTrigger.next();
                    return;
                }

                String hostUuid = getHostUuidFromVmInstanceUuid(vmInstanceUuid);
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.rescanLun(hostUuid, blockScsiLunVO.getInstallPath(), new Completion(flowTrigger) {
                    @Override
                    public void success() {
                        dbf.update(blockScsiLunVO);
                        flowTrigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        flowTrigger.fail(errorCode);
                    }
                });

            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                completion.fail(errorCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map map) {
                completion.success();
            }
        }).start();
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {

        List<VolumeInventory> blockVolumes = spec.getDestDataVolumes().stream()
                .filter(vol -> isBlockPrimaryStorage(vol.getInstallPath(), vol.getPrimaryStorageUuid()))
                .collect(Collectors.toList());

        if (isBlockPrimaryStorage(spec.getDestRootVolume().getInstallPath(), spec.getDestRootVolume().getPrimaryStorageUuid())) {
            blockVolumes.add(spec.getDestRootVolume());
        }

        if (blockVolumes.isEmpty()) {
            completion.success();
            return;
        }

        List<BlockScsiLunVO> blockScsiLunVOList = Q.New(BlockScsiLunVO.class)
                .in(BlockScsiLunVO_.volumeUuid, blockVolumes.stream().map(VolumeInventory::getUuid).collect(Collectors.toList()))
                .list();

        if (blockScsiLunVOList == null || blockScsiLunVOList.isEmpty()) {
            completion.fail(operr(String.format("can not find block scsi lun for volume list:%s", blockVolumes)));
            return;
        }

        new While<>(blockScsiLunVOList).each((BlockScsiLunVO blockScsiLunVO, WhileCompletion whileCompletion) -> {
            VolumeInventory volumeInventory = blockVolumes.stream().filter(volumeInventory1 -> volumeInventory1.getUuid().equals(blockScsiLunVO.getVolumeUuid())).findFirst().get();

            BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(spec.getDestHost().getUuid(), volumeInventory.getPrimaryStorageUuid());
            BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                    .eq(BlockPrimaryStorageVO_.uuid, volumeInventory.getPrimaryStorageUuid())
                    .find();
            BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);

            Map data = new HashMap();
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
            data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
            final Boolean[] alreadyMapped = {false};

            FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
            chain.setName("init-lun-map-before-start-vm");
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    bkd.checkLunHasMappedToHost(blockPrimaryStorageHostRefVO, blockScsiLunVO, new ReturnValueCompletion<Boolean>(whileCompletion) {
                        @Override
                        public void success(Boolean returnValue) {
                            alreadyMapped[0] = returnValue;
                            if (returnValue == false) {
                                logger.debug(String.format("lun:%s is not mapped to the host:%s", blockScsiLunVO.getWwn(), blockPrimaryStorageHostRefVO.getHostUuid()));
                            } else {
                                logger.debug(String.format("lun:%s has been mapped to the host:%s", blockScsiLunVO.getWwn(), blockPrimaryStorageHostRefVO.getHostUuid()));
                            }
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
            if (!alreadyMapped[0] && spec.getSrcHost() != null) {
                BlockPrimaryStorageHostRefVO lastBlockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(spec.getSrcHost().getUuid(), volumeInventory.getPrimaryStorageUuid());
                data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, lastBlockPrimaryStorageHostRefVO);
                chain.setData(data);
                chain.then(new DeleteLunMapFlow());
            }
            data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);
            chain.setData(data);
            chain.then(new MapLunToHostFlow());
            chain.error(new FlowErrorHandler(whileCompletion) {
                @Override
                public void handle(ErrorCode errorCode, Map map) {
                    whileCompletion.addError(errorCode);
                    whileCompletion.allDone();
                }
            }).done(new FlowDoneHandler(whileCompletion) {
                @Override
                public void handle(Map data) {
                    whileCompletion.done();
                }
            }).start();

        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void afterChangeVmVolumeInstallPath(String oldVolumeUuid, VolumeInventory newVol, Completion completion) {
        if (!isBlockPrimaryStorage(newVol.getInstallPath(), newVol.getPrimaryStorageUuid())) {
            completion.success();
            return;
        }
        BlockScsiLunVO oldBlockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, oldVolumeUuid)
                .find();

        BlockScsiLunVO newBlockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, newVol.getUuid())
                .find();

        oldBlockScsiLunVO.setVolumeUuid(newVol.getUuid());
        newBlockScsiLunVO.setVolumeUuid(oldVolumeUuid);
        try {
            dbf.update(oldBlockScsiLunVO);
            dbf.update(newBlockScsiLunVO);
        } catch (PersistenceException e) {
            completion.fail(operr("fail to exchange block scsi lun info:%s", e.getCause().toString()));
            return;
        }
        completion.success();
    }

    @Override
    public void innerOverwriteVolume(VolumeInventory originVolume, VolumeInventory transientVolume, VolumeDeletionPolicyManager.VolumeDeletionPolicy originVolumeDeletionPolicy) {
    }

    @Override
    public void afterOverwriteVolume(VolumeInventory volume, VolumeInventory transientVolume) {
        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, transientVolume.getPrimaryStorageUuid())
                .find();

        if (blockPrimaryStorageVO == null) {
            return;
        }

        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, transientVolume.getUuid())
                .find();

        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);
        OverwriteVolumeReply reply = new OverwriteVolumeReply();
        bkd.updateLunName(blockScsiLunVO, blockScsiLunVO.getName(), new Completion(reply) {
            @Override
            public void success() {
                logger.debug(String.format("successfully update lun name to:%s", blockScsiLunVO.getName()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to update lun name to:%s :%n%s",
                        blockScsiLunVO.getName(), errorCode.getReadableDetails()));
            }
        });
    }

    @Override
    public void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, Map data) {
        BlockPrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), BlockPrimaryStorageVO.class);
        if (pvo == null) {
            return;
        }

        if (pvo.getType().equals(type.toString())) {
            cmd.setVolume(convertVolumeToBlockIfNeeded(volume, cmd.getVolume()));
            logger.debug(String.format("cmd volume install path:%s", cmd.getVolume().getInstallPath()));
        }
    }

    @Override
    public void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd) {

    }

    @Override
    public void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.AttachDataVolumeCmd cmd, ErrorCode err, Map data) {

    }

    @Override
    public VolumeTO convertVolumeIfNeed(KVMHostInventory host, VolumeInventory inventory, VolumeTO to) {
        return convertVolumeToBlockIfNeeded(inventory, to);
    }

    @Override
    public void beforeDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        PrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (pvo == null) {
            return;
        }
        if (pvo.getType().equals(type.toString())) {
            cmd.setVolume(convertVolumeToBlockIfNeeded(volume, cmd.getVolume()));
        }
    }

    @Override
    public void afterDetachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd) {
        BlockPrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), BlockPrimaryStorageVO.class);
        if (pvo == null) {
            return;
        }
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(pvo);
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, volume.getUuid())
                .find();

        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(host.getUuid(), volume.getPrimaryStorageUuid());

        Map data = new HashMap();
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
        data.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete lun map after detach volume");
        chain.setData(data);
        chain.then(new DeleteLunMapFlow());
        chain.error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                throw new OperationFailureException(operr("fail to clean up after detach volume")
                        .withCause(errorCode));
            }
        }).done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map map) {

            }
        }).start();
    }

    @Override
    public void detachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, KVMAgentCommands.DetachDataVolumeCmd cmd, ErrorCode err) {

    }

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg cmsg, TakeVolumesSnapshotOnKvmMsg onKvmMsg, Map flowData, Completion completion) {
        List<CreateVolumesSnapshotsJobStruct> blockPSStructs = new ArrayList<>();
        for (CreateVolumesSnapshotsJobStruct struct : cmsg.getVolumeSnapshotJobs()) {
            if (Q.New(BlockPrimaryStorageVO.class)
                    .eq(BlockPrimaryStorageVO_.uuid, struct.getPrimaryStorageUuid())
                    .isExists()) {
                blockPSStructs.add(struct);
                onKvmMsg.getSnapshotJobs().removeIf(it -> it.getVolumeUuid().equals(struct.getVolumeUuid()));
            }
        }

        if (blockPSStructs.isEmpty()) {
            completion.success();
            return;
        }

        if (onKvmMsg.getSnapshotJobs().isEmpty()) {
            flowData.put(VolumeSnapshotConstant.NEED_BLOCK_STREAM_ON_HYPERVISOR, false);
            flowData.put(VolumeSnapshotConstant.NEED_TAKE_SNAPSHOTS_ON_HYPERVISOR, false);
        } else if (cmsg.getConsistentType() != ConsistentType.None) {
            completion.fail(operr("not support take volumes snapshots " +
                    "on multiple ps when including ceph"));
            return;
        }

        logger.info(String.format("take snapshots for volumes[%s] on %s",
                cmsg.getLockedVolumeUuids(), getClass().getCanonicalName()));

        ErrorCodeList errList = new ErrorCodeList();
        new While<>(blockPSStructs).all((struct, whileCompletion) -> {
            VolumeSnapshotVO vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, struct.getResourceUuid()).find();
            if (vo.getStatus().equals(VolumeSnapshotStatus.Ready)) {
                logger.warn(String.format("snapshot %s on volume %s is ready, no need to create again!",
                        vo.getUuid(), vo.getVolumeUuid()));
                whileCompletion.done();
                return;
            }
            TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
            tmsg.setPrimaryStorageUuid(struct.getPrimaryStorageUuid());
            tmsg.setStruct(struct.getVolumeSnapshotStruct());
            bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, tmsg.getPrimaryStorageUuid());
            bus.send(tmsg, new CloudBusCallBack(cmsg) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }
                    TakeSnapshotReply treply = reply.castReply();
                    if (!treply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        whileCompletion.done();
                        return;
                    }

                    vo.setPrimaryStorageInstallPath(treply.getInventory().getPrimaryStorageInstallPath());
                    vo.setSize(treply.getInventory().getSize());
                    vo.setPrimaryStorageUuid(treply.getInventory().getPrimaryStorageUuid());
                    vo.setType(treply.getInventory().getType());
                    vo.setFormat(treply.getInventory().getFormat());
                    vo.setStatus(VolumeSnapshotStatus.Ready);
                    dbf.update(vo);

                    struct.getVolumeSnapshotStruct().setCurrent(treply.getInventory());
                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public WorkflowTemplate createTemplateFromVolumeSnapshot(ParamIn paramIn) {
        WorkflowTemplate template = new WorkflowTemplate();

        PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, paramIn.getPrimaryStorageUuid())
                .eq(PrimaryStorageVO_.type, BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE)
                .find();
        if (ps == null) {
            return template;
        }

        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, paramIn.getSnapshot().getVolumeUuid())
                .find();

        if (blockScsiLunVO == null) {
            return template;
        }

        BlockScsiLunVO newBlockScsiLunVO = new BlockScsiLunVO();

        VolumeVO volumeVO = Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, paramIn.getSnapshot().getVolumeUuid())
                .find();
        blockScsiLunVO.setSize(volumeVO.getSize());

        String hostUuid = getHostUuidFromVmInstanceUuid(volumeVO.getAttachedVmUuids().get(0));
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(ps.getUuid());

        String finalHostUuid = hostUuid;
        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(finalHostUuid, ps.getUuid());
        String newImageName = generateImageCacheLunName(paramIn.getImage().getUuid(), paramIn.getPrimaryStorageUuid());
        template.setCreateTemporaryTemplate(new Flow() {
            String __name__ = "create new snap lun and map it to host";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLunFromTemplate(blockScsiLunVO, newImageName, getVolumeProvisioningStrategy(ps.getUuid()), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        newBlockScsiLunVO.setId(returnValue.getId());
                        newBlockScsiLunVO.setSize(returnValue.getSize());
                        newBlockScsiLunVO.setLunType(returnValue.getLunType());
                        newBlockScsiLunVO.setId(returnValue.getId());
                        newBlockScsiLunVO.setWwn(returnValue.getWwn());
                        bkd.createLunMap(newBlockScsiLunVO, blockPrimaryStorageHostRefVO, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                            @Override
                            public void success(BlockScsiLunVO returnValue) {
                                newBlockScsiLunVO.setTarget(returnValue.getTarget());
                                newBlockScsiLunVO.setId(returnValue.getId());
                                newBlockScsiLunVO.setLunMapId(returnValue.getLunMapId());
                                ImageCacheVO cvo = new ImageCacheVO();
                                cvo.setMd5sum("not calculated");
                                cvo.setSize(newBlockScsiLunVO.getSize());
                                cvo.setInstallUrl(newBlockScsiLunVO.getInstallPath());
                                cvo.setImageUuid(paramIn.getImage().getUuid());
                                cvo.setPrimaryStorageUuid(ps.getUuid());
                                cvo.setMediaType(ImageConstant.ImageMediaType.valueOf(paramIn.getImage().getMediaType()));
                                cvo.setState(ImageCacheState.ready);
                                cvo.setSize(newBlockScsiLunVO.getSize());
                                cvo = dbf.persistAndRefresh(cvo);
                                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                                blockPrimaryStorageKvmCommandDispatcher.discoverLun(finalHostUuid, newBlockScsiLunVO, bkd.getIscsiServer(newBlockScsiLunVO), new Completion(trigger) {
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

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (newBlockScsiLunVO.getId() != null ) {
                    BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(finalHostUuid, ps.getUuid());
                    bkd.deleteLunMap(newBlockScsiLunVO, blockPrimaryStorageHostRefVO, new Completion(trigger) {
                        @Override
                        public void success() {
                            bkd.deleteLun(newBlockScsiLunVO.getId(), new Completion(trigger) {
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

                        @Override
                        public void fail(ErrorCode errorCode) {
                            bkd.deleteLun(newBlockScsiLunVO.getId(), new Completion(trigger) {
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
                } else {
                    trigger.rollback();
                }
            }
        });

        template.setUploadToBackupStorage(new Flow() {
            String __name__ = "upload-to-backup-storage";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                BackupStorageAskInstallPathMsg ask = new BackupStorageAskInstallPathMsg();

                ImageStoreBackupStorageBlockKvmUploader uploader = new ImageStoreBackupStorageBlockKvmUploader(
                        PrimaryStorageInventory.valueOf(ps), paramIn.getBackupStorageUuid());

                ask.setImageUuid(paramIn.getImage().getUuid());
                ask.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                ask.setImageMediaType(paramIn.getImage().getMediaType());
                bus.makeTargetServiceIdByResourceUuid(ask, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                MessageReply ar = bus.call(ask);
                if (!ar.isSuccess()) {
                    trigger.fail(ar.getError());
                    return;
                }

                String bsInstallPath = ((BackupStorageAskInstallPathReply) ar).getInstallPath();
                uploader.uploadBits(paramIn.getImage().getUuid(), bsInstallPath, newBlockScsiLunVO.getInstallPath(), finalHostUuid, new ReturnValueCompletion<String>(trigger) {
                    @Override
                    public void success(String s) {
                        out.setBackupStorageInstallPath(s);
                        out.setSize(newBlockScsiLunVO.getSize());
                        out.setActualSize(newBlockScsiLunVO.getSize());
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
                final ParamOut out = (ParamOut) data.get(ParamOut.class);
                if (out.getBackupStorageInstallPath() != null) {
                    DeleteBitsOnBackupStorageMsg msg = new DeleteBitsOnBackupStorageMsg();
                    msg.setInstallPath(out.getBackupStorageInstallPath());
                    msg.setBackupStorageUuid(paramIn.getBackupStorageUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, paramIn.getBackupStorageUuid());
                    bus.send(msg);
                }
                trigger.rollback();
            }
        });

        template.setDeleteTemporaryTemplate(new NopeFlow());
        return template;
    }

    @Override
    public String createTemplateFromVolumeSnapshotPrimaryStorageType() {
        return BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public List<Flow> markRootVolumeAsSnapshot(VolumeInventory volumeInventory, VolumeSnapshotVO volumeSnapshotVO, String accountUuid) {
        Flow flow = new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
                cmsg.setAccountUuid(accountUuid);
                cmsg.setVolumeUuid(volumeInventory.getUuid());
                cmsg.setName(volumeInventory.getName());
                cmsg.setDescription(volumeInventory.getDescription());

                bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeSnapshotConstant.SERVICE_ID, volumeInventory.getUuid()) ;
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        CreateVolumeSnapshotReply r = (CreateVolumeSnapshotReply) reply;
                        volumeSnapshotVO.setType(VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString());
                        volumeSnapshotVO.setPrimaryStorageInstallPath(r.getInventory().getPrimaryStorageInstallPath());
                        volumeSnapshotVO.setUuid(r.getInventory().getUuid());
                        trigger.next();
                    }
                });
            }
        };
        return Collections.singletonList(flow);
    }

    @Override
    public String getExtensionPrimaryStorageType() {
        return BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public String kvmSetupSelfFencerStorageType() {
        return BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void kvmSetupSelfFencer(KvmSetupSelfFencerParam param, Completion completion) {
        SetupSelfFencerOnKvmHostMsg msg = new SetupSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });

    }

    @Override
    public void kvmCancelSelfFencer(KvmCancelSelfFencerParam param, Completion completion) {
        CancelSelfFencerOnKvmHostMsg msg = new CancelSelfFencerOnKvmHostMsg();
        msg.setParam(param);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, param.getPrimaryStorage().getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        if (host.getClusterUuid() == null) {
            completion.done();
            return;
        }

        List<String> psUuid = SQL.New("select ps.uuid from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref where " +
                "ps.type = :blockPrimaryType and " +
                "ref.clusterUuid = :hostClusterUuid and " +
                "ps.uuid = ref.primaryStorageUuid")
                .param("blockPrimaryType", BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE)
                .param("hostClusterUuid", host.getClusterUuid()).list();

        if (psUuid == null || psUuid.isEmpty()) {
            logger.debug("host doesn't access to any ps just return");
            completion.done();
            return;
        }

        List<String> psHeartbeatLunName = psUuid.stream().map(this::generateHeartbeatLunName).collect(Collectors.toList());
        List<BlockScsiLunVO> psHeartbeatLuns = Q.New(BlockScsiLunVO.class)
                .in(BlockScsiLunVO_.name, psHeartbeatLunName)
                .list();

        List<String> psHeartbeatInstallPath = psHeartbeatLuns.stream().map(BlockScsiLunVO::getInstallPath).collect(Collectors.toList());


        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
        blockPrimaryStorageKvmCommandDispatcher.pingAgentNoFailure(host.getUuid(), psHeartbeatInstallPath, new ReturnValueCompletion<BlockPrimaryStorageKvmCommandDispatcher.PingAgentNoFailureRsp>(completion) {
            @Override
            public void success(BlockPrimaryStorageKvmCommandDispatcher.PingAgentNoFailureRsp returnValue) {
                for (BlockScsiLunVO psHeartbeatLun : psHeartbeatLuns) {
                    String psUuid = getPrimaryStorageUuidFromHeartbeatLun(psHeartbeatLun);
                    if (returnValue.getDisconnectedPSInstallPath() != null &&
                            returnValue.getDisconnectedPSInstallPath().contains(psHeartbeatLun.getInstallPath())) {
                        changeBlockPrimaryStorageHostRefStatus(psUuid, host.getUuid(), PrimaryStorageHostStatus.Disconnected);
                    } else {
                        changeBlockPrimaryStorageHostRefStatus(psUuid, host.getUuid(), PrimaryStorageHostStatus.Connecting);
                    }
                }
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        });
    }

    public void changeBlockPrimaryStorageHostRefStatus(String primaryStorageUuid, String hostUuid, PrimaryStorageHostStatus status) {
        PrimaryStorageHostRefVO vo = Q.New(PrimaryStorageHostRefVO.class)
                .eq(PrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .find();
        if (vo == null) {
            logger.warn(String.format("can not find ref of host uuid %s and ps %s", hostUuid, primaryStorageUuid));
            return;
        }

        PrimaryStorageHostStatus oldStatus = vo.getStatus();
        if (oldStatus.equals(status)) {
            return;
        }
        vo.setStatus(status);
        vo = dbf.updateAndRefresh(vo);

        if (!oldStatus.equals(vo.getStatus())) {
            PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData data = new PrimaryStorageCanonicalEvent.PrimaryStorageHostStatusChangeData();
            data.setHostUuid(hostUuid);
            data.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            data.setOldStatus(oldStatus);
            data.setNewStatus(vo.getStatus());
            evtf.fire(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH, data);
        }
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    private void mapVolumeToVMHost(VmInstanceInventory vm, VolumeInventory volume) {
        // volume is not instantiated, yet, just return
        if (volume.getPrimaryStorageUuid() == null) {
            return;
        }

        if (!isBlockPrimaryStorage(volume.getInstallPath(), volume.getPrimaryStorageUuid())) {
            return;
        }
        BlockPrimaryStorageVO pvo = dbf.findByUuid(volume.getPrimaryStorageUuid(), BlockPrimaryStorageVO.class);
        if (pvo == null) {
            return;
        }
        BlockPrimaryStorageDeviceBackend bkd = getBlockPrimaryStorageDeviceBackend(pvo);

        String hostUuid = getHostUuidFromVmInstanceUuid(vm.getUuid());
        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = getBlockPrimaryStorageHostRefVO(hostUuid, volume.getPrimaryStorageUuid());
        BlockScsiLunVO blockScsiLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.volumeUuid, volume.getUuid())
                .find();
        AttachDataVolumeToVmMsg attachDataVolumeToVmMsg = new AttachDataVolumeToVmMsg();
        FutureCompletion completion = new FutureCompletion(null);

        Map newChainData = new HashMap();
        newChainData.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageBackend, bkd);
        newChainData.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, blockScsiLunVO);
        newChainData.put(BlockPrimaryStorageConstants.Params.BlockPrimaryStorageHostRef, blockPrimaryStorageHostRefVO);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("before-attach-volume-on-block-ps");
        chain.setData(newChainData);
        chain.then(new MapLunToHostFlow());
        chain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("all pre works have been done"));
                completion.success();
            }
        }).error(new FlowErrorHandler(attachDataVolumeToVmMsg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                throw new OperationFailureException(operr("fail to map lun to host before attach volume to vm"));
            }
        }).start();
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data) {
        //TODO delete old lun map
        mapVolumeToVMHost(vm, volume);
    }

    @Override
    public void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {
        mapVolumeToVMHost(vm, volume);
    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data) {

    }

    public VolumeProvisioningStrategy getVolumeProvisioningStrategy(String primaryStorageUuid) {
        VolumeProvisioningStrategy volumeProvisioningStrategy = VolumeProvisioningStrategy.ThickProvisioning;
        String provisioningStrategy = VolumeSystemTags
                .PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY.getTokenByResourceUuid(
                        primaryStorageUuid, PrimaryStorageVO.class,
                        VolumeSystemTags.PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN);
        if (provisioningStrategy == null) {
            return volumeProvisioningStrategy;
        }
        if (VolumeProvisioningStrategy.valueOf(provisioningStrategy).equals(VolumeProvisioningStrategy.ThinProvisioning)) {
            volumeProvisioningStrategy = VolumeProvisioningStrategy.ThinProvisioning;
        }

        return volumeProvisioningStrategy;
    }

    public String generateHeartbeatLunName(String primaryStorageUuid) {
        String heartbeatLunName = BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_NAME_PREFIX + primaryStorageUuid;
        return heartbeatLunName;
    }

    public String generateImageCacheLunName(String imageUuid, String primaryStorageUuid) {
        String imageCacheLunName = String.format("%s%s-%s", BlockPrimaryStorageConstants.BLOCK_IMAGE_CACHE_LUN_NAME_PREFIX,
                primaryStorageUuid, imageUuid);
        return imageCacheLunName;
    }

    public String getPrimaryStorageUuidFromHeartbeatLun(BlockScsiLunVO heartbeatLun) {
        String psUuid = heartbeatLun.getName().replaceAll(BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_NAME_PREFIX, "");
        return psUuid;
    }

    public BlockScsiLunVO generateHeartbeatLun(String primaryStorageUuid) {
        String heartbeatLunName = generateHeartbeatLunName(primaryStorageUuid);
        BlockScsiLunVO heartbeatLunVO = Q.New(BlockScsiLunVO.class)
                .eq(BlockScsiLunVO_.name, heartbeatLunName)
                .find();
        if (heartbeatLunVO == null) {
            heartbeatLunVO = new BlockScsiLunVO();
            heartbeatLunVO.setSize(BlockPrimaryStorageGlobalConfig.BLOCK_PRIMARY_STORAGE_HEARTBEAT_LUN_SIZE.value(Long.class));
            heartbeatLunVO.setName(heartbeatLunName);
        }
        return  heartbeatLunVO;
    }

    public void createHeartbeatLunIfNotExist(String primaryStorageUuid, BlockPrimaryStorageDeviceBackend bkd, ReturnValueCompletion<BlockScsiLunVO> completion) {
        final BlockScsiLunVO heartbeatLunVO = generateHeartbeatLun(primaryStorageUuid);
        if (StringUtils.isEmpty(heartbeatLunVO.getUuid())) {
            heartbeatLunVO.setUuid(Platform.getUuid());
            dbf.persist(heartbeatLunVO);
        }

        final Boolean[] heartbeatLunAlreadyCreated = {false};
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-heartbeat-lun-for-ps-%s-if-not-exist", primaryStorageUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "query heart beat lun";
            @Override
            public void run(FlowTrigger trigger, Map data) {

                bkd.getLunByName(heartbeatLunVO.getName(), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        heartbeatLunVO.setWwn(returnValue.getWwn());
                        heartbeatLunVO.setTarget(returnValue.getTarget());
                        heartbeatLunVO.setLunMapId(returnValue.getLunMapId());
                        heartbeatLunVO.setId(returnValue.getId());
                        SQL.New(BlockScsiLunVO.class)
                                .eq(BlockScsiLunVO_.name, heartbeatLunVO.getName())
                                .set(BlockScsiLunVO_.wwn, returnValue.getWwn())
                                .set(BlockScsiLunVO_.target, returnValue.getTarget())
                                .set(BlockScsiLunVO_.id, returnValue.getId())
                                .update();
                        heartbeatLunAlreadyCreated[0] = true;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create heartbeat lun";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLun(heartbeatLunVO, getVolumeProvisioningStrategy(primaryStorageUuid), new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        heartbeatLunVO.setWwn(returnValue.getWwn());
                        heartbeatLunVO.setLunType(returnValue.getLunType());
                        heartbeatLunVO.setId(returnValue.getId());
                        SQL.New(BlockScsiLunVO.class)
                                .eq(BlockScsiLunVO_.name, heartbeatLunVO.getName())
                                .set(BlockScsiLunVO_.wwn, returnValue.getWwn())
                                .set(BlockScsiLunVO_.id, returnValue.getId())
                                .set(BlockScsiLunVO_.lunType, returnValue.getLunType())
                                .update();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (errorCode.isError(LunErrors.LUN_HAS_BEEN_CREATED)) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCode);
                        }
                    }
                });
            }

            @Override
            public boolean skip(Map map) {
                return heartbeatLunAlreadyCreated[0];
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(heartbeatLunVO);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    public List<BlockPrimaryStorageHostRefVO> saveBlockPrimaryStorageHostsRefIfNotExist(List<HostVO> hosts, String psUuid) {
        if (hosts.isEmpty()) {
            return edu.emory.mathcs.backport.java.util.Collections.emptyList();
        }

        List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS = Q.New(BlockPrimaryStorageHostRefVO.class)
                .in(BlockPrimaryStorageHostRefVO_.hostUuid, hosts.stream()
                        .map(HostVO::getUuid).collect(Collectors.toList()))
                .eq(BlockPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid).list();

        Set<String> hostAlreadyAdded = blockPrimaryStorageHostRefVOS.stream().map(BlockPrimaryStorageHostRefVO::getHostUuid).collect(Collectors.toSet());
        hosts.stream().filter(h -> !hostAlreadyAdded.contains(h.getUuid())).forEach(host -> {
            BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = new BlockPrimaryStorageHostRefVO();
            blockPrimaryStorageHostRefVO.setHostUuid(host.getUuid());
            blockPrimaryStorageHostRefVO.setPrimaryStorageUuid(psUuid);
            blockPrimaryStorageHostRefVO.setStatus(PrimaryStorageHostStatus.Connecting);
            blockPrimaryStorageHostRefVO.setCreateDate(new Timestamp(DocUtils.date));
            blockPrimaryStorageHostRefVO = dbf.persistAndRefresh(blockPrimaryStorageHostRefVO);
            blockPrimaryStorageHostRefVOS.add(blockPrimaryStorageHostRefVO);
        });

        return blockPrimaryStorageHostRefVOS;
    }

    public RunInQueue lunMapOperationInQueue() {
        return new RunInQueue("block-lunmap-operation-in-queue", thdf, 1);
    }

    public BlockPrimaryStorageHostRefVO getBlockPrimaryStorageHostRefVO(String hostUuid, String primaryStorageUuid) {
        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = Q.New(BlockPrimaryStorageHostRefVO.class)
                .eq(BlockPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(BlockPrimaryStorageHostRefVO_.primaryStorageUuid, primaryStorageUuid)
                .find();
        return  blockPrimaryStorageHostRefVO;
    }

    private String getHostUuidFromVmInstanceUuid(String vmInstanceUuid) {
        Tuple hostInfo = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findTuple();

        String hostUuid = hostInfo.get(0, String.class);
        String lastHost = hostInfo.get(1, String.class);
        if (hostUuid == null || hostUuid.equals("")) {
            hostUuid = lastHost;
        }

        return hostUuid;
    }

    private String getVMRelatedIsoImageCachePrimaryStorageUuid(VmInstanceInventory inv, String isoUuid) {
        String vmPrimaryStorageUuid = inv.getRootVolume().getPrimaryStorageUuid();
        Boolean samePs = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.imageUuid, isoUuid)
                .eq(ImageCacheVO_.primaryStorageUuid, vmPrimaryStorageUuid)
                .isExists();
        if (samePs) {
            return vmPrimaryStorageUuid;
        }

        List<ImageCacheVO> imageCaches = Q.New(ImageCacheVO.class)
                .select(ImageCacheVO_.primaryStorageUuid)
                .eq(ImageCacheVO_.imageUuid, isoUuid)
                .list();

        List<ImageCacheVO> imageCachesInBPS = imageCaches.stream().filter(it -> it.getInstallUrl().startsWith("block://")).collect(Collectors.toList());
        if (imageCachesInBPS == null || imageCachesInBPS.isEmpty()) {
            return "";
        }

        return imageCachesInBPS.get(0).getPrimaryStorageUuid();
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion) {
        completion.success();
    }
}