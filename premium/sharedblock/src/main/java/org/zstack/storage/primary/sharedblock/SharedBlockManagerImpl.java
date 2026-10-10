package org.zstack.storage.primary.sharedblock;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
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
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageDiscoverExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageDiscoveryResult;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotState;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        } else if (msg instanceof APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg) {
            handle((APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg) msg);
        } else if (msg instanceof APIGetSharedBlockVolumeLanFreeLayoutMsg) {
            handle((APIGetSharedBlockVolumeLanFreeLayoutMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg msg) {
        List<SharedBlockVolumeSnapshotLanFreeTarget> targets;
        try {
            SharedBlockVolumeSnapshotLanFreeLayoutValidator.validateRequest(
                    msg.getSnapshotUuids());
            List<String> inaccessibleSnapshotUuids =
                    SharedBlockVolumeSnapshotAccessPolicy.findInaccessible(
                            acntMgr.isInSystemAdminType(msg.getSession()),
                            msg.getSession().getAccountUuid(),
                            msg.getSnapshotUuids(),
                            SharedBlockVolumeSnapshotAccessPolicy::filterAccessibleResources);
            if (!inaccessibleSnapshotUuids.isEmpty()) {
                replyError(msg, org.zstack.core.Platform.err(
                        SysErrors.RESOURCE_NOT_ACCESSIBLE,
                        "volume snapshots%s are inaccessible for account[uuid:%s]",
                        inaccessibleSnapshotUuids,
                        msg.getSession().getAccountUuid()));
                return;
            }
            targets = resolveVolumeSnapshotLanFreeTargets(
                    msg.getSnapshotUuids());
            SharedBlockVolumeSnapshotLanFreeLayoutValidator.validateResolvedTargets(
                    targets, loadLanFreePrimaryStorageTypes(targets));
        } catch (IllegalArgumentException e) {
            replyError(msg, argerr(e.getMessage()));
            return;
        }

        Map<String, List<SharedBlockVolumeSnapshotLanFreeTarget>> targetsByPrimaryStorage =
                new LinkedHashMap<>();
        for (SharedBlockVolumeSnapshotLanFreeTarget target : targets) {
            targetsByPrimaryStorage.computeIfAbsent(
                    target.getPrimaryStorageUuid(), ignored -> new ArrayList<>()).add(target);
        }

        List<GetSharedBlockVolumeSnapshotLanFreeLayoutMsg> internalMessages = new ArrayList<>();
        for (Map.Entry<String, List<SharedBlockVolumeSnapshotLanFreeTarget>> entry
                : targetsByPrimaryStorage.entrySet()) {
            GetSharedBlockVolumeSnapshotLanFreeLayoutMsg imsg =
                    new GetSharedBlockVolumeSnapshotLanFreeLayoutMsg();
            imsg.setPrimaryStorageUuid(entry.getKey());
            imsg.setTargets(entry.getValue());
            bus.makeTargetServiceIdByResourceUuid(
                    imsg, PrimaryStorageConstant.SERVICE_ID, entry.getKey());
            internalMessages.add(imsg);
        }

        bus.send(internalMessages, Math.min(5, internalMessages.size()),
                new CloudBusListCallBack(msg) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply reply : replies) {
                            if (!reply.isSuccess()) {
                                replyError(msg, reply.getError());
                                return;
                            }
                        }

                        List<GetSharedBlockVolumeSnapshotLanFreeLayoutReply> backendReplies =
                                new ArrayList<>();
                        for (MessageReply reply : replies) {
                            backendReplies.add(reply.castReply());
                        }

                        SharedBlockVolumeSnapshotLanFreeLayoutAggregator.Result result;
                        try {
                            result = SharedBlockVolumeSnapshotLanFreeLayoutAggregator.aggregate(
                                    targets, backendReplies);
                        } catch (IllegalArgumentException e) {
                            replyError(msg, operr(e.getMessage()));
                            return;
                        }

                        APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply apiReply =
                                new APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply();
                        apiReply.setLuns(result.getLuns());
                        apiReply.setLayouts(result.getLayouts());
                        bus.reply(msg, apiReply);
                    }
                });
    }

    private List<SharedBlockVolumeSnapshotLanFreeTarget> resolveVolumeSnapshotLanFreeTargets(
            List<String> snapshotUuids) {
        List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class)
                .in(VolumeSnapshotVO_.uuid, snapshotUuids).list();
        Map<String, VolumeSnapshotVO> snapshotsByUuid = new HashMap<>();
        for (VolumeSnapshotVO vo : vos) {
            snapshotsByUuid.put(vo.getUuid(), vo);
        }

        List<SharedBlockVolumeSnapshotLanFreeTarget> targets = new ArrayList<>();
        for (int index = 0; index < snapshotUuids.size(); index++) {
            String snapshotUuid = snapshotUuids.get(index);
            VolumeSnapshotVO source = snapshotsByUuid.get(
                    snapshotUuid);
            if (source == null) {
                throw new IllegalArgumentException(String.format(
                        "cannot find volume snapshot[uuid:%s]",
                        snapshotUuid));
            }
            if (source.getStatus() != VolumeSnapshotStatus.Ready
                    || source.getState() != VolumeSnapshotState.Enabled) {
                throw new IllegalArgumentException(String.format(
                        "volume snapshot[uuid:%s] must be Ready and Enabled, current status[%s], state[%s]",
                        source.getUuid(), source.getStatus(), source.getState()));
            }
            if (source.getVolumeUuid() == null
                    || source.getPrimaryStorageUuid() == null
                    || source.getPrimaryStorageInstallPath() == null) {
                throw new IllegalArgumentException(String.format(
                        "volume snapshot[uuid:%s] has incomplete primary storage identity",
                        source.getUuid()));
            }

            SharedBlockVolumeSnapshotLanFreeTarget target =
                    new SharedBlockVolumeSnapshotLanFreeTarget();
            target.setVolumeSnapshotUuid(source.getUuid());
            target.setVolumeUuid(source.getVolumeUuid());
            target.setRequestIndex(index);
            target.setPrimaryStorageUuid(source.getPrimaryStorageUuid());
            target.setVolumeSnapshotInstallPath(
                    source.getPrimaryStorageInstallPath());
            targets.add(target);
        }
        return targets;
    }

    private Map<String, String> loadLanFreePrimaryStorageTypes(
            List<SharedBlockVolumeSnapshotLanFreeTarget> targets) {
        Set<String> psUuids = targets.stream()
                .map(SharedBlockVolumeSnapshotLanFreeTarget::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, String> result = new HashMap<>();
        if (psUuids.isEmpty()) {
            return result;
        }

        List<PrimaryStorageVO> vos = Q.New(PrimaryStorageVO.class)
                .in(PrimaryStorageVO_.uuid, psUuids).list();
        for (PrimaryStorageVO vo : vos) {
            result.put(vo.getUuid(), vo.getType());
        }
        return result;
    }

    private void replyError(
            APIGetSharedBlockVolumeSnapshotLanFreeLayoutMsg msg, ErrorCode error) {
        APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply reply =
                new APIGetSharedBlockVolumeSnapshotLanFreeLayoutReply();
        reply.setError(error);
        bus.reply(msg, reply);
    }

    private void handle(APIGetSharedBlockVolumeLanFreeLayoutMsg msg) {
        List<SharedBlockVolumeLanFreeTarget> targets;
        try {
            SharedBlockVolumeLanFreeLayoutValidator.validateRequest(msg.getVolumeUuids());
            List<String> inaccessibleVolumeUuids =
                    SharedBlockVolumeAccessPolicy.findInaccessible(
                            acntMgr.isInSystemAdminType(msg.getSession()),
                            msg.getSession().getAccountUuid(),
                            msg.getVolumeUuids(),
                            SharedBlockVolumeAccessPolicy::filterAccessibleResources);
            if (!inaccessibleVolumeUuids.isEmpty()) {
                replyError(msg, org.zstack.core.Platform.err(
                        SysErrors.RESOURCE_NOT_ACCESSIBLE,
                        "volumes%s are inaccessible for account[uuid:%s]",
                        inaccessibleVolumeUuids,
                        msg.getSession().getAccountUuid()));
                return;
            }
            targets = resolveVolumeLanFreeTargets(msg.getVolumeUuids());
            SharedBlockVolumeLanFreeLayoutValidator.validateResolvedTargets(
                    targets, loadVolumeLanFreePrimaryStorageTypes(targets));
        } catch (IllegalArgumentException e) {
            replyError(msg, argerr(e.getMessage()));
            return;
        }

        Map<String, List<SharedBlockVolumeLanFreeTarget>> targetsByPrimaryStorage =
                new LinkedHashMap<>();
        for (SharedBlockVolumeLanFreeTarget target : targets) {
            targetsByPrimaryStorage.computeIfAbsent(
                    target.getPrimaryStorageUuid(), ignored -> new ArrayList<>()).add(target);
        }

        List<GetSharedBlockVolumeLanFreeLayoutMsg> internalMessages = new ArrayList<>();
        for (Map.Entry<String, List<SharedBlockVolumeLanFreeTarget>> entry
                : targetsByPrimaryStorage.entrySet()) {
            GetSharedBlockVolumeLanFreeLayoutMsg imsg =
                    new GetSharedBlockVolumeLanFreeLayoutMsg();
            imsg.setPrimaryStorageUuid(entry.getKey());
            imsg.setTargets(entry.getValue());
            bus.makeTargetServiceIdByResourceUuid(
                    imsg, PrimaryStorageConstant.SERVICE_ID, entry.getKey());
            internalMessages.add(imsg);
        }

        bus.send(internalMessages, Math.min(5, internalMessages.size()),
                new CloudBusListCallBack(msg) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        for (MessageReply reply : replies) {
                            if (!reply.isSuccess()) {
                                replyError(msg, reply.getError());
                                return;
                            }
                        }

                        List<GetSharedBlockVolumeLanFreeLayoutReply> backendReplies =
                                new ArrayList<>();
                        for (MessageReply reply : replies) {
                            backendReplies.add(reply.castReply());
                        }

                        SharedBlockVolumeLanFreeLayoutAggregator.Result result;
                        try {
                            result = SharedBlockVolumeLanFreeLayoutAggregator.aggregate(
                                    targets, backendReplies);
                        } catch (IllegalArgumentException e) {
                            replyError(msg, operr(e.getMessage()));
                            return;
                        }

                        APIGetSharedBlockVolumeLanFreeLayoutReply apiReply =
                                new APIGetSharedBlockVolumeLanFreeLayoutReply();
                        apiReply.setLuns(result.getLuns());
                        apiReply.setLayouts(result.getLayouts());
                        bus.reply(msg, apiReply);
                    }
                });
    }

    private List<SharedBlockVolumeLanFreeTarget> resolveVolumeLanFreeTargets(
            List<String> volumeUuids) {
        List<VolumeVO> vos = Q.New(VolumeVO.class)
                .in(VolumeVO_.uuid, volumeUuids).list();
        Map<String, VolumeVO> volumesByUuid = new HashMap<>();
        for (VolumeVO vo : vos) {
            volumesByUuid.put(vo.getUuid(), vo);
        }

        List<SharedBlockVolumeLanFreeTarget> targets = new ArrayList<>();
        for (int index = 0; index < volumeUuids.size(); index++) {
            String volumeUuid = volumeUuids.get(index);
            VolumeVO source = volumesByUuid.get(volumeUuid);
            if (source == null) {
                throw new IllegalArgumentException(String.format(
                        "cannot find volume[uuid:%s]", volumeUuid));
            }
            if (source.getStatus() != VolumeStatus.Ready
                    || source.getState() != VolumeState.Enabled) {
                throw new IllegalArgumentException(String.format(
                        "volume[uuid:%s] must be Ready and Enabled, current status[%s], state[%s]",
                        source.getUuid(), source.getStatus(), source.getState()));
            }
            if (source.getPrimaryStorageUuid() == null
                    || source.getInstallPath() == null) {
                throw new IllegalArgumentException(String.format(
                        "volume[uuid:%s] has incomplete primary storage identity",
                        source.getUuid()));
            }
            SharedBlockVolumeLanFreeTarget target =
                    new SharedBlockVolumeLanFreeTarget();
            target.setVolumeUuid(source.getUuid());
            target.setRequestIndex(index);
            target.setPrimaryStorageUuid(source.getPrimaryStorageUuid());
            target.setVolumeInstallPath(source.getInstallPath());
            targets.add(target);
        }
        return targets;
    }

    private Map<String, String> loadVolumeLanFreePrimaryStorageTypes(
            List<SharedBlockVolumeLanFreeTarget> targets) {
        Set<String> psUuids = targets.stream()
                .map(SharedBlockVolumeLanFreeTarget::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, String> result = new HashMap<>();
        if (psUuids.isEmpty()) {
            return result;
        }
        List<PrimaryStorageVO> vos = Q.New(PrimaryStorageVO.class)
                .in(PrimaryStorageVO_.uuid, psUuids).list();
        for (PrimaryStorageVO vo : vos) {
            result.put(vo.getUuid(), vo.getType());
        }
        return result;
    }

    private void replyError(
            APIGetSharedBlockVolumeLanFreeLayoutMsg msg, ErrorCode error) {
        APIGetSharedBlockVolumeLanFreeLayoutReply reply =
                new APIGetSharedBlockVolumeLanFreeLayoutReply();
        reply.setError(error);
        bus.reply(msg, reply);
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
    public void discoverStrangePrimaryStorage(String clusterUuid, ReturnValueCompletion<PrimaryStorageDiscoveryResult> completion) {
        List<String> hostUuids = Q.New(HostVO.class).eq(HostVO_.clusterUuid, clusterUuid)
                .eq(HostVO_.status, HostStatus.Connected)
                .notIn(HostVO_.state, Arrays.asList(HostState.Maintenance, HostState.PreMaintenance))
                .select(HostVO_.uuid)
                .listValues();
        if (CollectionUtils.isEmpty(hostUuids)) {
            completion.success(new PrimaryStorageDiscoveryResult());
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

    private void discoverDisksFromHosts(String clusterUuid, List<String> hostUuids, ReturnValueCompletion<PrimaryStorageDiscoveryResult> completion) {
        Map<String, SharedBlockKvmCommands.GetVgsInfoRsp> hostResults = new ConcurrentHashMap<>();
        Set<String> allVgUuids = new HashSet<>();
        Map<String, Set<String>> attachedSharedBlockWwids = getAttachedSharedBlockWwids(clusterUuid);
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
                    if (rsp.vgDiskInfos != null) {
                        rsp.vgDiskInfos.stream()
                                .filter(Objects::nonNull)
                                .filter(info -> !isAttachedSharedBlockDisk(info, attachedSharedBlockWwids))
                                .map(info -> info.vgUuid)
                                .filter(Objects::nonNull)
                                .forEach(allVgUuids::add);
                    }
                });

                Set<String> existingSharedBlockGroupUuids = getExistingSharedBlockGroupUuids(allVgUuids);

                List<PrimaryStorageInventory> inventories = new ArrayList<>();
                List<String> resetVgUuidRequiredUuids = new ArrayList<>();
                for (String vgUuid : allVgUuids) {
                    DiskAggregation aggregation = collectDiskAggregation(
                            vgUuid, hostUuids, hostResults, attachedSharedBlockWwids);

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
                    if (existingSharedBlockGroupUuids.contains(vgUuid)) {
                        resetVgUuidRequiredUuids.add(vgUuid);
                    }
                }

                PrimaryStorageDiscoveryResult result = new PrimaryStorageDiscoveryResult();
                result.setInventories(inventories);
                result.setResetVgUuidRequiredUuids(resetVgUuidRequiredUuids);
                completion.success(result);
            }
        });
    }

    private Set<String> getExistingSharedBlockGroupUuids(Collection<String> vgUuids) {
        if (isEmpty(vgUuids)) {
            return Collections.emptySet();
        }

        List<String> existingUuids = Q.New(SharedBlockGroupVO.class)
                .select(SharedBlockGroupVO_.uuid)
                .in(SharedBlockGroupVO_.uuid, vgUuids)
                .listValues();
        return new HashSet<>(existingUuids);
    }

    private Map<String, Set<String>> getAttachedSharedBlockWwids(String clusterUuid) {
        List<String> attachedSharedBlockUuids = SQL.New("select sbg.uuid" +
                " from PrimaryStorageClusterRefVO ref, SharedBlockGroupVO sbg" +
                " where ref.clusterUuid = :clusterUuid" +
                " and ref.primaryStorageUuid = sbg.uuid", String.class)
                .param("clusterUuid", clusterUuid)
                .list();

        Map<String, Set<String>> ret = new HashMap<>();
        if (isEmpty(attachedSharedBlockUuids)) {
            return ret;
        }

        List<Tuple> diskTuples = Q.New(SharedBlockVO.class)
                .select(SharedBlockVO_.sharedBlockGroupUuid, SharedBlockVO_.diskUuid)
                .in(SharedBlockVO_.sharedBlockGroupUuid, attachedSharedBlockUuids)
                .listTuple();

        for (Tuple tuple : diskTuples) {
            String vgUuid = tuple.get(0, String.class);
            String diskUuid = normalizeWwid(tuple.get(1, String.class));
            if (diskUuid == null) {
                continue;
            }

            ret.computeIfAbsent(vgUuid, k -> new HashSet<>()).add(diskUuid);
        }

        return ret;
    }

    private boolean isAttachedSharedBlockDisk(SharedBlockKvmCommands.SharedBlockVgDiskInfo diskInfo,
                                              Map<String, Set<String>> attachedSharedBlockWwids) {
        if (diskInfo == null || diskInfo.vgUuid == null || diskInfo.disk == null) {
            return false;
        }

        Set<String> attachedWwids = attachedSharedBlockWwids.get(diskInfo.vgUuid);
        String discoveredWwid = normalizeWwid(diskInfo.disk.wwid);
        return discoveredWwid != null && attachedWwids != null && attachedWwids.contains(discoveredWwid);
    }

    private String normalizeWwid(String wwid) {
        if (wwid == null) {
            return null;
        }

        String normalized = wwid.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private DiskAggregation collectDiskAggregation(String vgUuid, List<String> hostUuids,
                                                    Map<String, SharedBlockKvmCommands.GetVgsInfoRsp> hostResults,
                                                    Map<String, Set<String>> attachedSharedBlockWwids) {
        DiskAggregation aggregation = new DiskAggregation();

        for (String hostUuid : hostUuids) {
            processHostVg(vgUuid, hostUuid, hostResults.get(hostUuid), attachedSharedBlockWwids, aggregation);
        }

        return aggregation;
    }

    private void processHostVg(String vgUuid, String hostUuid, SharedBlockKvmCommands.GetVgsInfoRsp rsp,
                               Map<String, Set<String>> attachedSharedBlockWwids, DiskAggregation aggregation) {
        if (rsp == null || rsp.vgDiskInfos == null) {
            aggregation.clusterComplete = false;
            aggregation.hostDiskIds.put(hostUuid, new ArrayList<>());
            return;
        }

        List<SharedBlockKvmCommands.SharedBlockVgDiskInfo> diskInfos = rsp.vgDiskInfos.stream()
                .filter(Objects::nonNull)
                .filter(info -> vgUuid.equals(info.vgUuid))
                .filter(info -> !isAttachedSharedBlockDisk(info, attachedSharedBlockWwids))
                .collect(Collectors.toList());
        if (diskInfos.isEmpty()) {
            aggregation.clusterComplete = false;
            aggregation.hostDiskIds.put(hostUuid, new ArrayList<>());
            return;
        }

        List<SharedBlockCandidateStruct> luns = diskInfos.stream()
                .map(info -> info.disk)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<Long> pvCounts = diskInfos.stream()
                .map(info -> info.diskCount)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Long pvCount = pvCounts.size() == 1 ? pvCounts.iterator().next() : null;

        Set<String> hostWwids = new HashSet<>();
        for (SharedBlockCandidateStruct lun : luns) {
            if (lun.wwid == null) {
                continue;
            }

            hostWwids.add(lun.wwid);
            aggregation.allDiskStructs.putIfAbsent(lun.wwid, lun);
        }

        boolean isLocalComplete = pvCount != null && pvCount == (long) hostWwids.size()
                && hostWwids.size() == luns.size();
        if (!isLocalComplete) {
            aggregation.clusterComplete = false;
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
