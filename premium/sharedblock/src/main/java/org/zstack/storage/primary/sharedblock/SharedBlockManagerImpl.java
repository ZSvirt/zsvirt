package org.zstack.storage.primary.sharedblock;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.PrimaryStorageDiscoverExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Create by weiwang at 2018/7/13
 */
public class SharedBlockManagerImpl extends AbstractService implements PrimaryStorageDiscoverExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SharedBlockManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;

    @Override
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
        if (msg instanceof APIGetSharedBlockCandidateMsg) {
            handle((APIGetSharedBlockCandidateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetSharedBlockCandidateMsg msg) {
        APIGetSharedBlockCandidateReply reply = new APIGetSharedBlockCandidateReply();
        reply.setResults(new ArrayList<>());

        List<HostVO> hostVOS = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .list();

        if (hostVOS == null || hostVOS.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        Multiset<BlockDeviceStruct> devices = ConcurrentHashMultiset.create();
        new While<>(hostVOS).step((hostVO, whileCompletion) -> {
            SharedBlockKvmCommands.GetBlockDevicesCmd cmd = new SharedBlockKvmCommands.GetBlockDevicesCmd();
            cmd.hostUuid = hostVO.getUuid();
            // psUuid=null: discovery operation not bound to a specific primary storage
            new KvmAgentCommandDispatcher(null, hostVO.getUuid()).go(SharedBlockKvmCommands.GET_BLOCK_DEVICES_PATH, cmd, SharedBlockKvmCommands.GetBlockDevicesRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.GetBlockDevicesRsp>(whileCompletion) {
                @Override
                public void success(SharedBlockKvmCommands.GetBlockDevicesRsp returnValue) {
                    devices.addAll(returnValue.blockDevices);
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                logger.debug("got sharedblock device info from all hosts, now remove attached devices");
                List<String> existsSharedBlockWwids = getExistsSharedBlockWwids();
                List<String> existsVmAttachWwids = getExistsVmAttachWwids();

                for (BlockDeviceStruct s : devices) {
                    Long count = devices.stream().filter(d -> d.equals(s)).count();

                    if (count < hostVOS.size() || reply.getResults().contains(new SharedBlockCandidateStruct(s))) {
                        continue;
                    }

                    if (existsSharedBlockWwids.contains(s.wwid) || existsSharedBlockWwids.contains(s.wwn)) {
                        continue;
                    }

                    if (existsVmAttachWwids.contains(s.wwid) || existsVmAttachWwids.contains(s.wwn)) {
                        continue;
                    }

                    reply.getResults().add(new SharedBlockCandidateStruct(s));
                }

                logger.debug("sharedblock devices calculate done, return result");
                bus.reply(msg, reply);
            }
        });
    }

    private List<String> getExistsSharedBlockWwids() {
        return Q.New(SharedBlockVO.class).select(SharedBlockVO_.diskUuid).listValues();
    }

    private List<String> getExistsVmAttachWwids() {
        return SQL.New("select lun.wwid from ScsiLunVmInstanceRefVO ref, ScsiLunVO lun where lun.uuid = ref.scsiLunUuid", String.class).list();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SharedBlockConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        installSystemTagValidator();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void installSystemTagValidator() {
        ThinProvisioningInitializeSizeValidator thinProvisioningInitializeSizeValidator = new ThinProvisioningInitializeSizeValidator();
        tagMgr.installCreateMessageValidator(VolumeVO.class.getSimpleName(), thinProvisioningInitializeSizeValidator);
        SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG.installValidator(thinProvisioningInitializeSizeValidator);
    }

    @SuppressWarnings("rawtypes")
    static class ThinProvisioningInitializeSizeValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
        @Override
        public void validateSystemTagInCreateMessage(APICreateMessage cmsg) {
            if (isEmpty(cmsg.getSystemTags())) {
                return;
            }

            String thinProvisioningInitializeTag = cmsg.getSystemTags().stream()
                    .filter(SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG::isMatch).findFirst().orElse(null);
            if (thinProvisioningInitializeTag == null) {
                return;
            }

            validateThinProvisioningInitializeSize(thinProvisioningInitializeTag);
        }

        @Override
        public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
            if (!SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG.isMatch(systemTag)) {
                return;
            }
            validateThinProvisioningInitializeSize(systemTag);
        }

        private void validateThinProvisioningInitializeSize(String systemTag) {
            String size = SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG.getTokenByTag(systemTag,
                    SharedBlockSystemTags.THIN_PROVISIONING_INITIALIZE_SIZE_TAG_TOKEN);

            if (size == null || size.trim().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("invalid thinProvisioningInitializeSize tag")
                        .withOpaque("tag", systemTag));
            }

            try {
                long val = new BigInteger(size).longValueExact();
                if (val < SharedBlockConstants.MIN_THIN_PROVISIONING_INITIALIZE_SIZE) {
                    throw new ApiMessageInterceptionException(argerr(
                            "invalid thinProvisioningInitializeSize tag, it must be greater than or equal to %s",
                            SharedBlockConstants.MIN_THIN_PROVISIONING_INITIALIZE_SIZE)
                            .withOpaque("tag", systemTag)
                            .withOpaque("greater.than", SharedBlockConstants.MIN_THIN_PROVISIONING_INITIALIZE_SIZE));
                }
            } catch (NumberFormatException e) {
                throw new ApiMessageInterceptionException(argerr(
                        "invalid thinProvisioningInitializeSize, it is not a number", size)
                        .withOpaque("tag", systemTag));
            } catch (ArithmeticException e) {
                throw new ApiMessageInterceptionException(argerr(
                        "invalid thinProvisioningInitializeSize is larger than %d", size, Long.MAX_VALUE)
                        .withOpaque("tag", systemTag));
            }
        }
    }

    @Override
    public void discoverStrangePrimaryStorage(String clusterUuid, ReturnValueCompletion<List<PrimaryStorageInventory>> completion) {
        List<String> hostUuids = Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .notIn(HostVO_.state, Arrays.asList(HostState.Maintenance, HostState.PreMaintenance))
                .select(HostVO_.uuid)
                .listValues();
        if (CollectionUtils.isEmpty(hostUuids)) {
            completion.success(new ArrayList<>());
            return;
        }

        new While<>(hostUuids).step((hostUuid, whileCompletion) -> {
            SharedBlockKvmCommands.CheckDisksCmd checkCmd = new SharedBlockKvmCommands.CheckDisksCmd();
            checkCmd.sharedBlockUuids = new ArrayList<>();
            checkCmd.rescan_scsi = true;
            checkCmd.hostUuid = hostUuid;
            new KvmAgentCommandDispatcher(null, hostUuid).go(SharedBlockKvmCommands.CHECK_DISKS_PATH, checkCmd,
                    SharedBlockKvmCommands.AgentRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.AgentRsp>(whileCompletion) {
                        @Override
                        public void success(SharedBlockKvmCommands.AgentRsp rsp) {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("checkDisk(rescan_scsi) failed on host[uuid:%s], %s", hostUuid, errorCode));
                            whileCompletion.done();
                        }
                    });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                discoverDisksFromHosts(clusterUuid, hostUuids, completion);
            }
        });
    }

    private void discoverDisksFromHosts(String clusterUuid, List<String> hostUuids, ReturnValueCompletion<List<PrimaryStorageInventory>> completion) {
        Map<String, SharedBlockKvmCommands.GetVgsInfoRsp> hostResults = new ConcurrentHashMap<>();
        Set<String> allVgUuids = new HashSet<>();
        new While<>(hostUuids).step((hostUuid, whileCompletion) -> {
            SharedBlockKvmCommands.GetVgsInfoCmd cmd = new SharedBlockKvmCommands.GetVgsInfoCmd();
            cmd.hostUuid = hostUuid;
            new KvmAgentCommandDispatcher(null, hostUuid).go(SharedBlockKvmCommands.VGS_ALL_PATH, cmd,
                    SharedBlockKvmCommands.GetVgsInfoRsp.class, new ReturnValueCompletion<SharedBlockKvmCommands.GetVgsInfoRsp>(whileCompletion) {
                        @Override
                        public void success(SharedBlockKvmCommands.GetVgsInfoRsp returnValue) {
                            hostResults.put(hostUuid, returnValue);
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to get VG info from host[uuid:%s]: %s", hostUuid, errorCode));
                            whileCompletion.done();
                        }
                    });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (hostResults.isEmpty()) {
                    completion.fail(operr("failed to get VG info from any host in cluster[uuid:%s]",
                            clusterUuid));
                    return;
                }

                hostResults.values().forEach(rsp -> {
                    if (rsp.groupDiskInfos != null) {
                        allVgUuids.addAll(rsp.groupDiskInfos.keySet());
                    }
                });

                Set<String> existingPsUuids = new HashSet<>(Q.New(PrimaryStorageVO.class)
                        .eq(PrimaryStorageVO_.type, SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)
                        .select(PrimaryStorageVO_.uuid).listValues());
                allVgUuids.removeAll(existingPsUuids);

                List<PrimaryStorageInventory> inventories = new ArrayList<>();
                for (String vgUuid : allVgUuids) {
                    DiskAggregation aggregation = collectDiskAggregation(vgUuid, hostUuids, hostResults);

                    logger.debug(String.format("discover VG[uuid:%s] existingDiskIdsByHost: %s",
                            vgUuid, aggregation.hostDiskIds));

                    SharedBlockGroupPrimaryStorageInventory inv = new SharedBlockGroupPrimaryStorageInventory();
                    inv.setUuid(vgUuid);
                    inv.setType(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
                    inv.setStatus(aggregation.clusterComplete
                            ? PrimaryStorageStatus.Connected.toString()
                            : PrimaryStorageStatus.Disconnected.toString());

                    List<SharedBlockInventory> sharedBlocks = new ArrayList<>();
                    if (aggregation.intersectedWwids != null) {
                        for (String wwid : aggregation.intersectedWwids) {
                            SharedBlockCandidateStruct disk = aggregation.allDiskStructs.get(wwid);
                            SharedBlockInventory sbi = new SharedBlockInventory();
                            sbi.setDiskUuid(disk.wwid);
                            sbi.setTotalCapacity(disk.size);
                            sbi.setVendor(disk.vendor);
                            sharedBlocks.add(sbi);
                        }
                    }
                    inv.setSharedBlocks(sharedBlocks);
                    inventories.add(inv);
                }

                completion.success(inventories);
            }
        });
    }

    private DiskAggregation collectDiskAggregation(String vgUuid, List<String> hostUuids, Map<String, SharedBlockKvmCommands.GetVgsInfoRsp> hostResults) {
        DiskAggregation aggregation = new DiskAggregation();

        for (String hostUuid : hostUuids) {
            processHostVg(vgUuid, hostUuid, hostResults.get(hostUuid), aggregation);
        }

        return aggregation;
    }

    private void processHostVg(String vgUuid, String hostUuid, SharedBlockKvmCommands.GetVgsInfoRsp rsp, DiskAggregation aggregation) {
        if (rsp == null || rsp.groupDiskInfos == null || !rsp.groupDiskInfos.containsKey(vgUuid)) {
            aggregation.clusterComplete = false;
            aggregation.hostDiskIds.put(hostUuid, new ArrayList<>());
            return;
        }

        SharedBlockKvmCommands.SharedBlockGroupDiskInfo diskInfo = rsp.groupDiskInfos.get(vgUuid);
        List<SharedBlockCandidateStruct> luns = diskInfo.disks;
        Long pvCount = diskInfo.diskCount;
        boolean isLocalComplete = luns != null && pvCount != null && pvCount == (long) luns.size();
        if (!isLocalComplete) {
            aggregation.clusterComplete = false;
        }

        if (luns == null) {
            aggregation.hostDiskIds.put(hostUuid, new ArrayList<>());
            return;
        }

        Set<String> hostWwids = new HashSet<>();
        for (SharedBlockCandidateStruct lun : luns) {
            if (lun.wwid == null) {
                continue;
            }

            hostWwids.add(lun.wwid);
            aggregation.allDiskStructs.putIfAbsent(lun.wwid, lun);
        }

        aggregation.hostDiskIds.put(hostUuid, new ArrayList<>(hostWwids));
        if (aggregation.intersectedWwids == null) {
            aggregation.intersectedWwids = new HashSet<>(hostWwids);
            return;
        }

        aggregation.intersectedWwids.retainAll(hostWwids);
    }

    private static class DiskAggregation {
        private boolean clusterComplete = true;
        private Set<String> intersectedWwids;
        private final Map<String, SharedBlockCandidateStruct> allDiskStructs = new HashMap<>();
        private final Map<String, List<String>> hostDiskIds = new HashMap<>();
    }
}
