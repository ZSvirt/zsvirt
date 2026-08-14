package org.zstack.storage.primary.block.vendor.xstor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.TakeSnapshotOnHypervisorReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.sdk.IscsiServerInventory;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.block.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/3/29 18:13
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XStorDeviceImpl implements BlockPrimaryStorageDeviceBackend {
    private static final CLogger logger = Utils.getLogger(XStorDeviceImpl.class);
    private static final String vendorName = "XStor";

    @Autowired
    public RESTFacade restf;

    @Autowired
    protected DatabaseFacade dbf;

    private XStorDevice xStorDevice = new XStorDevice();

    private String metadata;

    public XStorDeviceImpl(){
    };

    public XStorDeviceImpl(String metadata) {
        XStorDevice x = new XStorDevice(metadata);
        this.xStorDevice.setIp(x.getIp());
        this.xStorDevice.setPort(x.getPort());
        this.xStorDevice.setUsername(x.getUsername());
        this.xStorDevice.setPassword(x.getPassword());
        this.xStorDevice.setStoragePools(x.getStoragePools());
        this.xStorDevice.setAccessZones(x.getAccessZones());
        this.metadata = metadata;
    }

    @Override
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getMetadata() {
        return metadata;
    }

    @Override
    public IscsiServerInventory getIscsiServer(BlockScsiLunVO blockScsiLunVO) {
        IscsiServerInventory inventory = new IscsiServerInventory();
        AccessZoneRsp.AccessZone accessZone = xStorDevice.getAccessZones().get(0);
        AccessZoneRsp.Subnet subnet = accessZone.getSubnets().get(0);
        String targetIp = getTargetLoginIP(blockScsiLunVO.getTarget(), subnet.getSvip());

        inventory.setIp(targetIp);
        inventory.setPort(3260);
        return inventory;
    }

    private String getTargetLoginIP(String target, String svip) {
        if (target == null) {
            return svip;
        }
        Pattern pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        Matcher matcher = pattern.matcher(target);
        if (matcher.find()){
            return matcher.group();
        } else {
            return svip;
        }
    }

    @Override
    public String syncMetadata() {
        List<StoragePoolRsp.StoragePool> storagePoolList = xStorDevice.syncStoragePools();
        List<AccessZoneRsp.AccessZone> accessZoneList = xStorDevice.syncAccessZones();
        XStorMetadata x = new XStorMetadata();
        x.setStoragePools(storagePoolList);
        x.setAccessZones(accessZoneList);
        x.setIp(xStorDevice.getIp());
        x.setPort(xStorDevice.getPort());
        x.setUsername(xStorDevice.getUsername());
        return JSONObjectUtil.toJsonString(x);
    }

    @Override
    public void createLun(BlockScsiLunVO blockScsiLunVO, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion) {
        try {
            BlockScsiLunVO b = xStorDevice.createLun(blockScsiLunVO, volumeProvisioningStrategy);
            completion.success(b);
        } catch (OperationFailureException exception) {
            completion.fail(exception.getErrorCode());
        }
    }

    @Override
    public void createLunMap(BlockScsiLunVO blockScsiLunVO, BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, ReturnValueCompletion<BlockScsiLunVO> completion) {
        String hostRefMetadataString = blockPrimaryStorageHostRefVO.getMetadata();
        Gson gson = new GsonBuilder().create();
        HostRefMetadata hostRefMetadata = gson.fromJson(hostRefMetadataString, HostRefMetadata.class);
        Integer hostGroupId = hostRefMetadata.getHostGroupId();
        Integer lunMapId = 0;
        try {
            lunMapId = xStorDevice.queryLunMap(hostGroupId, blockScsiLunVO.getId());
            if (lunMapId.equals(0)) {
                lunMapId = xStorDevice.createLunMap(hostGroupId, blockScsiLunVO.getId(), blockScsiLunVO.getLunType());
            }
        } catch (OperationFailureException exception) {
            completion.fail(exception.getErrorCode());
            return;
        }

        List<Integer> ids = new ArrayList<>();
        ids.add(blockScsiLunVO.getId());

        List<LunMap> lunMaps = xStorDevice.queryLunMap(Collections.singletonList(lunMapId));
        // query lunmap by id, only 1 lunmap returned.
        // save latest lunmap id
        LunMap lunMap = lunMaps.get(0);
        blockScsiLunVO.setLunMapId(lunMap.getId());

        // query lun by id, only 1 lun returned
        // Currently doesn't support multi path
        List<BlockScsiLunVO> blockScsiLunVOList = xStorDevice.queryLun(ids);
        blockScsiLunVO.setTarget(blockScsiLunVOList.get(0).getTarget());
        completion.success(blockScsiLunVO);
    }

    @Override
    public void deleteLunMap(BlockScsiLunVO blockScsiLunVO, BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, Completion completion) {

        if (blockScsiLunVO.getId() == null || blockScsiLunVO.getId() == 0) {
            completion.fail(operr("There is no way to get lun map id, we just return as failure"));
            return;
        }
        Gson gson = new GsonBuilder().create();
        HostRefMetadata metadataMap = gson.fromJson(blockPrimaryStorageHostRefVO.getMetadata(), HostRefMetadata.class);
        Integer hostGroupId = metadataMap.getHostGroupId();
        Integer lunMapId = xStorDevice.queryLunMap(hostGroupId, blockScsiLunVO.getId());

        if (Integer.valueOf(0).equals(lunMapId)) {
            completion.fail(operr("lun map id is mandatory can not be null, neither 0"));
            return;
        }
        xStorDevice.deleteLunMap(lunMapId);
        completion.success();
    }

    @Override
    public void checkLunHasMappedToHost(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<Boolean> completion) {
        String metadata = blockPrimaryStorageHostRefVO.getMetadata();
        Gson gson = new GsonBuilder().create();
        HostRefMetadata metadataMap = gson.fromJson(metadata, HostRefMetadata.class);
        Integer hostGroupId = metadataMap.getHostGroupId();
        Integer lunMapId = xStorDevice.queryLunMap(hostGroupId, blockScsiLunVO.getId());
        if (lunMapId == 0 || lunMapId == null) {
            completion.success(false);
            return;
        }
        completion.success(true);
    }

    @Override
    public void deleteLun(Integer id, Completion completion) {
        if (id == null || id == 0) {
            completion.fail(operr("lun id is illegal"));
            return;
        }
        // tryDelete api support delete snapshot by default
        //List<Snapshot> snapshotList = xStorDevice.querySnapshot(Collections.singletonList(id), XStorDevice.BLOCK_RE_LUN);

        //if (snapshotList.size() == 0) {
        //    snapshotList = xStorDevice.querySnapshot(Collections.singletonList(id), XStorDevice.BLOCK_RE_CLONE);
        //}
        //for (Snapshot snapshot : snapshotList) {
        //    xStorDevice.deleteSnapshot(snapshot.getId());
        //}
        Lun lun = new Lun();
        try {
            lun = xStorDevice.queryLun(id);
        } catch (OperationFailureException e) {
            if (e.getErrorCode().getDetails().equals(LunErrors.LUN_CAN_NOT_BE_FOUND.toString())) {
                completion.success();
                return;
            }
            completion.fail(e.getErrorCode());
            return;
        }
        for (LunMap lunMap : lun.getLunMaps()) {
            xStorDevice.deleteLunMap(lunMap.getId());
        }
        xStorDevice.deleteLun(id);
        completion.success();
    }

    public class HostRefMetadata {
        private Integer hostId;
        private Integer hostGroupId;

        public Integer getHostGroupId() {
            return hostGroupId;
        }

        public Integer getHostId() {
            return hostId;
        }

        public void setHostGroupId(Integer hostGroupId) {
            this.hostGroupId = hostGroupId;
        }

        public void setHostId(Integer hostId) {
            this.hostId = hostId;
        }
    }

    @Override
    public void addHost(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, ReturnValueCompletion<BlockPrimaryStorageHostRefVO> completion) {
        String hostUuid = blockPrimaryStorageHostRefVO.getHostUuid();
        Integer groupId = xStorDevice.addHostGroup(hostUuid);
        if (groupId == 0) {
            completion.fail(operr("fail to add host group: %s", hostUuid));
            return;
        }
        Integer hostId = xStorDevice.addHost(groupId, hostUuid);
        if (hostId == 0) {
            completion.fail(operr("fail to add host: %s", hostUuid));
            return;
        }
        HostRefMetadata hostRefMetadata = new HostRefMetadata();
        hostRefMetadata.setHostId(hostId);
        hostRefMetadata.setHostGroupId(groupId);
        blockPrimaryStorageHostRefVO.setMetadata(JSONObjectUtil.toJsonString(hostRefMetadata));

        Initiator initiator = xStorDevice.getInitiatorByName(blockPrimaryStorageHostRefVO.getInitiatorName());
        if (initiator != null && !hostUuid.equals(initiator.hostName)) {
            completion.fail(operr(String.format("initiator[%s] is existed in XStor and isn't attached to the host[%s], please make sure initiator name is unique", blockPrimaryStorageHostRefVO.getInitiatorName(), hostUuid)));
            return;
        }

        if (initiator == null) {
            xStorDevice.addInitiator(hostId, blockPrimaryStorageHostRefVO.getInitiatorName());
        }
        logger.debug(String.format("Add host initiator: %s to XStor", JSONObjectUtil.toJsonString(blockPrimaryStorageHostRefVO)));
        completion.success(blockPrimaryStorageHostRefVO);
    }

    @Override
    public void deleteHost(String metadata, Completion completion) {
        Gson gson = new GsonBuilder().create();
        HostRefMetadata metadataMap = gson.fromJson(metadata, HostRefMetadata.class);
        Integer hostGroupId = metadataMap.getHostGroupId();
        Integer hostId = metadataMap.getHostId();
        List<Host> hostList = new ArrayList<>();
        try{
            hostList = xStorDevice.queryHost(Collections.singletonList(hostId));
        } catch (OperationFailureException e) {
            logger.debug(String.format("fail to get host info from XStor, because of %s, we can just return as success", e.getMessage()));
        }

        if (hostList == null || hostList.isEmpty()) {
            completion.success();
            return;
        }

        List<LunMap> lunMaps = xStorDevice.queryLunMap(hostGroupId);
        if (!lunMaps.isEmpty()) {
            lunMaps.forEach(lunMap -> {
                try{
                    xStorDevice.deleteLunMap(lunMap.getId());
                } catch (OperationFailureException eee) {
                    logger.debug(String.format("fail to delete lun map %s, just ignore this", eee.getMessage()));
                }
            });
        }

        Host host = hostList.get(0);
        if (!host.getInitiatorIds().isEmpty()) {
            host.getInitiatorIds().forEach(initiatorId -> {
                try {
                    xStorDevice.deleteInitiator(initiatorId);
                } catch (OperationFailureException e) {
                    completion.fail(e.getErrorCode());
                    return;
                }
            });
        }

        try {
            xStorDevice.deleteHost(hostId);
            xStorDevice.deleteHostGroup(hostGroupId);
        } catch (OperationFailureException ee) {
            completion.fail(ee.getErrorCode());
            return;
        }
        completion.success();
    }

    @Override
    public void createHeartbeat(List<BlockPrimaryStorageHostRefVO> blockPrimaryStorageHostRefVOS, String primaryStorageUuid, BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<BlockScsiLunVO> completion) {
        for (BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO : blockPrimaryStorageHostRefVOS) {
            String metadata = blockPrimaryStorageHostRefVO.getMetadata();
            Gson gson = new GsonBuilder().create();
            HostRefMetadata metadataMap = gson.fromJson(metadata, HostRefMetadata.class);
            Integer hostGroupId = metadataMap.getHostGroupId();
            xStorDevice.createLunMap(hostGroupId, blockScsiLunVO.getId(), "LUN");
        }

        List<Integer> ids = new ArrayList<>();
        ids.add(blockScsiLunVO.getId());
        List<BlockScsiLunVO> blockScsiLunVOList = xStorDevice.queryLun(ids);
        blockScsiLunVO.setTarget(blockScsiLunVOList.get(0).getTarget());
        blockScsiLunVO.setLunType(XStorDevice.BLOCK_RE_LUN);
        completion.success(blockScsiLunVO);
    }

    @Override
    public void createLunFromTemplate(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion) {
        try {
            BlockScsiLunVO newBlockScsiLunVO = xStorDevice.createLunFromTemplate(blockScsiLunVO, name, volumeProvisioningStrategy);
            completion.success(newBlockScsiLunVO);
        } catch (OperationFailureException exception) {
            completion.fail(exception.getErrorCode());
        }
    }

    @Override
    public void createLunFromSnapshot(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy, ReturnValueCompletion<BlockScsiLunVO> completion) {
        try {
            BlockScsiLunVO newBlockScsiLunVO = xStorDevice.createLunFromSnapshot(blockScsiLunVO, name, volumeProvisioningStrategy);
            completion.success(newBlockScsiLunVO);
        } catch (OperationFailureException exception) {
            completion.fail(exception.getErrorCode());
        }
    }

    @Override
    public void resizeLun(BlockScsiLunVO blockScsiLunVO, long resize, Completion completion) {
        xStorDevice.updateLun(blockScsiLunVO.getName(), resize, blockScsiLunVO.getId());
        completion.success();
    }

    @Override
    public void updateLunName(BlockScsiLunVO blockScsiLunVO, String newName, Completion completion) {
        xStorDevice.updateLun(newName, blockScsiLunVO.getSize(), blockScsiLunVO.getId());
        completion.success();
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public BlockPrimaryStorageDeviceBackend getBlockPrimaryStorageDeviceBackend(BlockPrimaryStorageVO bpsvo) {
        if (!bpsvo.getVendorName().equals(vendorName)) {
            return null;
        }
        XStorDeviceImpl xStorDeviceImpl = new XStorDeviceImpl(bpsvo.getMetadata());
        return xStorDeviceImpl;
    }

    @Override
    public void getPhysicalCapacityUsage(ReturnValueCompletion<PrimaryStorageBase.PhysicalCapacityUsage> completion) {
        PrimaryStorageBase.PhysicalCapacityUsage physicalCapacityUsage = new PrimaryStorageBase.PhysicalCapacityUsage();
        Integer storagePoolId = xStorDevice.getStoragePools().get(0).getId();
        StoragePoolRsp.StoragePool storagePool = xStorDevice.getStoragePool(storagePoolId);
        physicalCapacityUsage.totalPhysicalSize = storagePool.getTotal_bytes();
        physicalCapacityUsage.availablePhysicalSize = storagePool.getAvail_bytes();
        completion.success(physicalCapacityUsage);
    }

    @Override
    public void getLunByName(String name, ReturnValueCompletion<BlockScsiLunVO> completion) {
        if (name == null) {
            completion.fail(operr(String.format("lun name is mandatory but get null")));
            return;
        }
        Lun lun = xStorDevice.queryLun(name);

        if (lun == null ) {
            completion.fail(err(LunErrors.LUN_CAN_NOT_BE_FOUND, "lun can not be found"));
            return;
        }
        BlockScsiLunVO blockScsiLunVO = lun.toBlockScsiLun();
        completion.success(blockScsiLunVO);
    }

    @Override
    public void takeSnapshot(BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<TakeSnapshotOnHypervisorReply> completion) {
        Integer snapshotId = xStorDevice.createSnapshot(blockScsiLunVO);

        List<Snapshot> snapshotList = xStorDevice.querySnapshot(Collections.singletonList(blockScsiLunVO.getId()), blockScsiLunVO.getLunType());
        if (snapshotList.size() < 1) {
            completion.fail(operr(String.format("fail to get snap shot for lun:%s", blockScsiLunVO.getName())));
            return;
        }

        Snapshot snapshot = snapshotList.stream().filter(s -> snapshotId.equals(s.getId())).findAny().orElse(null);
        if (snapshot == null) {
            completion.fail(operr(String.format("fail to get snapshot for lun:%s of id is :%s", blockScsiLunVO.getName(), String.valueOf(snapshotId))));
            return;
        }
        TakeSnapshotOnHypervisorReply takeSnapshotOnHypervisorReply = new TakeSnapshotOnHypervisorReply();
        takeSnapshotOnHypervisorReply.setSize(snapshot.getShareBytes());
        takeSnapshotOnHypervisorReply.setSnapshotInstallPath(String.valueOf(snapshotId));

        completion.success(takeSnapshotOnHypervisorReply);
        return;
    }

    @Override
    public void deleteSnapshot(DeleteSnapshotOnPrimaryStorageMsg msg, Completion completion) {
        List<Integer> ids = new ArrayList<>();
        String installPath = msg.getSnapshot().getPrimaryStorageInstallPath();
        Integer snapshotId = 0;
        try {
            snapshotId = Integer.valueOf(installPath);
            ids.add(snapshotId);
        } catch (Exception e) {}

        List<Snapshot> snapshotList = xStorDevice.querySnapshot(ids, "BLOCK_RE_CLONE");

        String name = msg.getSnapshot().getUuid();
        for (Snapshot snapshot : snapshotList) {
            if (snapshot.getName().contains(name)) {
                snapshotId = snapshot.getId();
            }
        }

        if (snapshotId == 0) {
            completion.success();
            return;
        }
        try {
            xStorDevice.deleteSnapshot(snapshotId);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }
        completion.success();
    }

    @Override
    public void revertSnapshot(BlockScsiLunVO blockScsiLunVO, VolumeSnapshotInventory snapshotInventory, ReturnValueCompletion<BlockScsiLunVO> completion) {
        String installPath = snapshotInventory.getPrimaryStorageInstallPath();
        Integer snapshotId = Integer.valueOf(installPath);
        try {
            Lun lun = xStorDevice.revertSnapshot(snapshotId, blockScsiLunVO.getId(), blockScsiLunVO.getLunType());
            blockScsiLunVO.setSize(lun.getTotalBytes());
            blockScsiLunVO.setUsedSize(lun.getUsedBytes());
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }
        completion.success(blockScsiLunVO);
    }

    @Override
    public void checkBits(String lunName, Long actualSize, ReturnValueCompletion<Boolean> completion) {
        final Integer[] lunId = {0};
        getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(completion) {
            @Override
            public void success(BlockScsiLunVO blockScsiLunVO) {
                lunId[0] = blockScsiLunVO.getId();
                Lun lun = xStorDevice.queryLun(lunId[0]);
                logger.debug(String.format("find get lun: %s", JSONObjectUtil.toJsonString(lun)));
                if (lun == null) {
                    completion.success(false);
                    return;
                }
                long usedBytes = lun.getUsedBytes();
                if (usedBytes >= actualSize) {
                    completion.success(true);
                } else {
                    completion.success(false);
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to get lun:%s:%n%s", lunName, errorCode.getReadableDetails()));
                completion.success(false);
            }
        });

    }

    @Override
    public void pingHook(Completion completion) {
        ClusterOverview clusterOverview = new ClusterOverview();
        try {
            clusterOverview = xStorDevice.queryCluster();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }
        if (clusterOverview.isClusterHealth()) {
            completion.success();
        } else {
            completion.fail(operr("XStor cluster is unhealthy, cluster info[cluster_data_state: %s, cluster_healthy_state: %s, cluster_running_state: %s]",
                    clusterOverview.getCluster_data_state(), clusterOverview.getCluster_healthy_state(), clusterOverview.getCluster_running_state()));
        }
    }

    @Override
    public void checkLunSession(BlockScsiLunVO blockScsiLunVO, String hostUuid, Completion completion) {
        if (blockScsiLunVO.getId() == null || blockScsiLunVO.getId().equals(0)) {
            completion.fail(operr("illegal lun id"));
            return;
        }

        try {
            Boolean isActive = xStorDevice.checkLunSession(blockScsiLunVO.getId(), hostUuid);
            if (!isActive) {
                completion.fail(operr(String.format("The session of lun %s with host %s is not active", String.valueOf(blockScsiLunVO.getId()), hostUuid)));
            } else {
                completion.success();
            }
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }
    }

    @Override
    public void getLunMappedHosts(BlockScsiLunVO blockScsiLunVO, ReturnValueCompletion<List<String>> completion) {
        if (blockScsiLunVO.getId() == null || blockScsiLunVO.getId().equals(0)) {
            completion.fail(operr("illegal lun id"));
            return;
        }
        try {
            List<LunMap> lunMaps = xStorDevice.getLunMapByLunId(blockScsiLunVO.getId());
            List<String> hostUuidList = lunMaps.stream().map(LunMap::getHostGroupName).collect(Collectors.toList());
            completion.success(hostUuidList);
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    @Override
    public void getImageCacheLun(String name, ReturnValueCompletion<BlockScsiLunVO> completion) {
        final Integer[] remain = {65};
        List<Integer> imageNameCandidate = Arrays.asList(0,1,2,3,4,5);
        List<Integer> indexTag = new ArrayList<>();
        VolumeProvisioningStrategy imageCacheLunProvisioning = VolumeProvisioningStrategy.ThinProvisioning;

        List<Lun> imageLunList = new ArrayList<>();
        List<ErrorCode> errorCodes = new ArrayList<>();
        new While<>(imageNameCandidate).each((Integer imageNameIndex, WhileCompletion whileCompletion) -> {
            String candidateImageName = name + '_' + String.valueOf(imageNameIndex);
            if ( imageNameIndex == 0) {
                candidateImageName = name;
            }
            Lun imageLun = xStorDevice.queryLun(candidateImageName);
            if (imageLun == null && imageNameIndex == 0) {
                errorCodes.add(operr(String.format("can not find lun: %s", name)));
                whileCompletion.allDone();
            }

            for (int t = -1; t < 30; t++) {
                if (imageLun != null && imageLun.isReady()) {
                    break;
                }
                logger.debug("since new image cache is not ready let's sleep for 2s, and get new image cache again");
                try {
                    TimeUnit.SECONDS.sleep(2l);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
                imageLun = xStorDevice.queryLun(candidateImageName);
            }

            if (imageLun == null || !imageLun.isReady()) {
                errorCodes.add(operr(String.format("new image cache is not ready please wait for a second, and retry again")));
                whileCompletion.allDone();
            }

            Lun finalImageLun = imageLun;
            xStorDevice.getRemainCreatedLunNumber(imageLun.getId(), XStorDevice.BLOCK_RE_LUN, new ReturnValueCompletion<Integer>(whileCompletion) {
                @Override
                public void success(Integer returnValue) {
                    remain[0] = returnValue;
                    if (remain[0] < 64 ) {
                        String newRoundImageName = name + '_' + String.valueOf(imageNameIndex + 1);
                        Lun lun = xStorDevice.queryLun(newRoundImageName);
                        if (lun == null) {
                            try {
                                finalImageLun.setRootType(XStorDevice.BLOCK_RE_LUN);
                                xStorDevice.copyLun(finalImageLun, newRoundImageName, imageCacheLunProvisioning);
                            } catch (OperationFailureException e) {
                                logger.debug("get error while prepare next image cache, because of " + e.getErrorCode().toString());
                            }
                        }
                    }
                    if (remain[0] < 2) {
                        whileCompletion.done();
                        return;
                    }

                    imageLunList.add(finalImageLun);
                    indexTag.add(imageNameIndex);
                    whileCompletion.allDone();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errorCodes.add(errorCode);
                    whileCompletion.allDone();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodes.isEmpty()) {
                    completion.fail(errorCodes.get(0));
                    return;
                }

                if (imageLunList.isEmpty()) {
                    completion.fail(operr("fail to get image cache lun info"));
                    return;
                }

                Lun imageLun = imageLunList.get(0);
                BlockScsiLunVO blockScsiLunVO = imageLun.toBlockScsiLun();

                if (indexTag.isEmpty()) {
                    logger.debug("no expired image cache need to be cleaned");
                    completion.success(blockScsiLunVO);
                    return;
                }
                for ( int j = indexTag.get(0) + 2; j < 6; j++ ) {
                    String expiredImageName = name + '_' + String.valueOf(j);
                    Lun expiredLun = xStorDevice.queryLun(expiredImageName);
                    if (expiredLun == null) {
                        break;
                    }
                    xStorDevice.deleteLun(expiredLun.getId());
                }
                completion.success(blockScsiLunVO);
            }
        });
    }
}
