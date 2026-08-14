package org.zstack.compute.vm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerJobVO_;
import org.zstack.header.securityLevel.SecurityLevel;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO;
import org.zstack.mevoco.ShareableVolumeVmInstanceRefVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by mingjian.deng on 16/10/28.
 */
public class VmMevocoApiInterceptor implements GlobalApiMessageInterceptor {
    protected static final CLogger logger = Utils.getLogger(VmMevocoApiInterceptor.class);
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acmgr;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmNicManager nicManager;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            APICreateVmInstanceFromTemplatedVmInstanceMsg cmsg = (APICreateVmInstanceFromTemplatedVmInstanceMsg) msg;
            bus.makeTargetServiceIdByResourceUuid(cmsg, MevocoConstants.SERVICE_ID, cmsg.getTemplatedVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeVmPasswordMsg) {
            validate((APIChangeVmPasswordMsg) msg);
        } else if (msg instanceof APISetNicQosMsg) {
            validate((APISetNicQosMsg) msg);
        } else if (msg instanceof APISetVmMonitorNumberMsg) {
            validate((APISetVmMonitorNumberMsg) msg);
        } else if (msg instanceof APIDeleteNicQosMsg) {
            validate((APIDeleteNicQosMsg) msg);
        } else if (msg instanceof APIGetNicQosMsg) {
            validate((APIGetNicQosMsg) msg);
        } else if (msg instanceof APIChangeVmImageMsg) {
            validate((APIChangeVmImageMsg) msg);
        } else if (msg instanceof APIUpdateVmNicMacMsg) {
            validate((APIUpdateVmNicMacMsg) msg);
        } else if (msg instanceof APISetVmCleanTrafficMsg) {
            validate((APISetVmCleanTrafficMsg) msg);
        } else if (msg instanceof APICreateTemplatedVmInstanceFromVmInstanceMsg) {
            validate((APICreateTemplatedVmInstanceFromVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromTemplatedVmInstanceMsg) {
            validate((APICreateVmInstanceFromTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APIConvertVmInstanceToTemplatedVmInstanceMsg) {
            validate((APIConvertVmInstanceToTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APICloneVmInstanceMsg) {
            validate((APICloneVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmSecurityLevelMsg) {
            validate((APISetVmSecurityLevelMsg) msg);
        } else if (msg instanceof APISetVmConsolePasswordMsg) {
            validate((APISetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APIGetVirtualizerInfoMsg) {
            validate((APIGetVirtualizerInfoMsg) msg);
        }

        return msg;
    }

    private void validate(APIConvertVmInstanceToTemplatedVmInstanceMsg msg) {
        ensureVmWithoutShareableVolume(msg.getVmInstanceUuid());
        checkVmSchedulerJob(msg.getVmInstanceUuid());
    }

    private void ensureVmWithoutShareableVolume(String vmInstanceUuid) {
        List<String> shareableVolumeUuids = Q.New(ShareableVolumeVmInstanceRefVO.class)
                .eq(ShareableVolumeVmInstanceRefVO_.vmInstanceUuid, vmInstanceUuid)
                .select(ShareableVolumeVmInstanceRefVO_.volumeUuid)
                .listValues();
        if (!shareableVolumeUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr(
                    "templated vm[uuid: %s] cannot be create from vm with shareable volume[uuids: %s]",
                    vmInstanceUuid, shareableVolumeUuids));
        }
    }

    private void checkVmSchedulerJob(String vmInstanceUuid) {
        String rootVolumeUuid = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, vmInstanceUuid)
                .eq(VolumeVO_.type, VolumeType.Root).select(VolumeVO_.uuid).findValue();
        List<Tuple> jobTuples = getJobUuidAndClassName(rootVolumeUuid);
        jobTuples.addAll(getJobUuidAndClassName(vmInstanceUuid));

        if (!CollectionUtils.isEmpty(jobTuples)) {
            StringBuilder jobsBuilder = new StringBuilder();
            jobTuples.forEach(job -> {
                jobsBuilder.append(String.format("jobUuid: %s, jobClassName: %s",
                        job.get(0, String.class), job.get(1, String.class))).append(", ");
                if (jobsBuilder.length() > 0) {
                    jobsBuilder.setLength(jobsBuilder.length() - 2);
                }
            });
            throw new ApiMessageInterceptionException(operr("failed to convert vm to templated vm, " +
                    "because the vm has scheduled jobs [%s]", jobsBuilder.toString()));
        }
    }

    private List<Tuple> getJobUuidAndClassName(String targetResourceUuid) {
        return Q.New(SchedulerJobVO.class).eq(SchedulerJobVO_.targetResourceUuid, targetResourceUuid)
                .select(SchedulerJobVO_.uuid, SchedulerJobVO_.jobClassName).listTuple();
    }

    private void validate(APICreateVmInstanceMsg msg) {
        if (msg.getDataDiskOfferingUuids() != null && msg.getDataDiskOfferingUuids().size() > KVMGlobalConfig.MAX_DATA_VOLUME_NUM.value(int.class)) {
            throw new ApiMessageInterceptionException(argerr("The number of data volumes exceeds the limit[num: %s], please reduce the number of data volumes during vm creation.", KVMGlobalConfig.MAX_DATA_VOLUME_NUM.value(int.class)));
        }

        List<String> systemTags = msg.getSystemTags();
        if (systemTags == null || systemTags.isEmpty()) {
            return;
        }
        VmPasswordStrengthConfig vmPasswordStrengthConfig = VmPasswordStrengthConfig.toObject(MevocoGlobalConfig.VM_CONSOLE_PASSWORD_STRENGTH_CHECK_CONFIG.value());

        for (String tag : systemTags) {
            if (VmSystemTags.CONSOLE_PASSWORD.isMatch(tag) && vmPasswordStrengthConfig.isCheckPasswordStrength()) {
                String consolePassword = VmSystemTags.CONSOLE_PASSWORD.getTokenByTag(tag, VmSystemTags.CONSOLE_PASSWORD_TOKEN);
                vmPasswordStrengthConfig.validatePasswordStrengthConfig(consolePassword);
            }
        }
    }

    private void validate(APISetVmConsolePasswordMsg msg) {
        VmPasswordStrengthConfig vmPasswordStrengthConfig = VmPasswordStrengthConfig.toObject(MevocoGlobalConfig.VM_CONSOLE_PASSWORD_STRENGTH_CHECK_CONFIG.value());
        if (vmPasswordStrengthConfig.isCheckPasswordStrength()) {
            vmPasswordStrengthConfig.validatePasswordStrengthConfig(msg.getConsolePassword());
        }
    }

    private void validate(APISetVmSecurityLevelMsg msg) {
        if (!MevocoGlobalConfig.ENABLE_SECURITY_LEVEL.value(Boolean.class)) {
            throw new ApiMessageInterceptionException(argerr("Failed to set security level, because security level is disabled."));
        }

        if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()) {
            throw new ApiMessageInterceptionException(argerr("Can not set security level to not %s vm [uuid:%s]", VmInstanceState.Stopped, msg.getVmInstanceUuid()));
        }

        if (msg.getSecurityLevel() == null) {
            return;
        }

        SecurityLevel level = SecurityLevel.fromCode(msg.getSecurityLevel());

        if (level == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown security level code[%s], supported values are %s", msg.getSecurityLevel(), SecurityLevel.values()));
        }
    }

    private void validate(APICloneVmInstanceMsg msg) {
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        checkVmInstanceType(vmInstanceVO.getType());
        msg.setVmInstanceInventory(VmInstanceInventory.valueOf(vmInstanceVO));

        if (!CollectionUtils.isEmpty(msg.getDiskAOs()) &&
                (msg.getPrimaryStorageUuidForDataVolume() != null || msg.getPrimaryStorageUuidForRootVolume() != null ||
                        !CollectionUtils.isEmpty(msg.getDataVolumeSystemTags()) || !CollectionUtils.isEmpty(msg.getRootVolumeSystemTags()))) {
            throw new ApiMessageInterceptionException(argerr("can not set primaryStorageUuidForRootVolume or " +
                    "primaryStorageUuidForDataVolume or rootVolumeSystemTags or dataVolumeSystemTags when diskAOs is not empty"));
        }

        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            List<VmNicParam> vmNicParams;
            try {
                vmNicParams = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }

            new VmNicParamValidator().withVmNicParams(vmNicParams)
                    .withSupportNicDriverTypes(nicManager.getSupportNicDriverTypes())
                    .withVmType(vmInstanceVO.getType())
                    .isWindowsVm(ImagePlatform.Windows.toString().equals(vmInstanceVO.getPlatform()))
                    .validate();

            new VmInstanceHelper().validateL3Networks(vmNicParams.stream().map(VmNicParam::getL3NetworkUuid).collect(Collectors.toList()));
        }

        if (!msg.getFull()) {
            return;
        }

        checkPrimaryStorageCapacity(msg.getNames().size(), vmInstanceVO);
    }

    private void checkVmInstanceType(String type) {
        if (!StringUtils.equals(VmInstanceConstant.USER_VM_TYPE, type)) {
            throw new ApiMessageInterceptionException(argerr("The operation only allows on user vm"));
        }
    }

    private void checkPrimaryStorageCapacity(int size, VmInstanceVO vmInstanceVO) {
        ListMultimap<String, VolumeVO> primaryStorageVolumesMap = ArrayListMultimap.create();
        for (VolumeVO volumeVO : vmInstanceVO.getAllVolumes()) {
            primaryStorageVolumesMap.put(volumeVO.getPrimaryStorageUuid(), volumeVO);
        }

        for (String primaryStorageUuid : primaryStorageVolumesMap.keySet()) {
            List<VolumeVO> volumeVOS = primaryStorageVolumesMap.get(primaryStorageUuid);

            Long totalCapacity = volumeVOS.stream().map(VolumeVO::getSize).reduce(0l, Long::sum);
            List<Long> snapshotsCapacities = Q.New(VolumeSnapshotVO.class)
                    .select(VolumeSnapshotVO_.size)
                    .in(VolumeSnapshotVO_.volumeUuid, volumeVOS.stream().map(VolumeVO::getUuid).collect(Collectors.toList()))
                    .listValues();
            Long snapshotsCapacity = snapshotsCapacities.stream().reduce(0l, Long::sum);


            PrimaryStorageVO primaryStorageVO = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, primaryStorageUuid).find();
            if (primaryStorageVO.getCapacity().getAvailableCapacity() < (totalCapacity - snapshotsCapacity) * size) {
                throw new ApiMessageInterceptionException(operr(
                        "there are not enough capacity for full vm clone to vm[uuid: %s], volumes[uuid: %s] on " +
                                "primary storage[uuid: %s] required: %s bytes, current available capacity is %s bytes",
                        vmInstanceVO.getUuid(), volumeVOS.stream().map(VolumeVO::getUuid).collect(Collectors.toList()),
                        primaryStorageUuid, (totalCapacity - snapshotsCapacity) * size, primaryStorageVO.getCapacity().getAvailableCapacity()
                ));
            }
        }
    }

    private void validate(APIUpdateVmNicMacMsg msg) {
        VmNicVO vo = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);
        VmInstanceVO vmInstanceVO = dbf.findByUuid(vo.getVmInstanceUuid(), VmInstanceVO.class);
        if (vmInstanceVO == null) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "The nic [%s%s] is not mounted on the VM",
                    msg.getVmNicUuid()
            ));
        }
        if (!StringUtils.equals(VmInstanceConstant.USER_VM_TYPE, vmInstanceVO.getType())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "The operation only allows on user vm "
            ));
        }
        if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped)) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "The operation only allows when vm [%s] state is stopped ",
                    vmInstanceVO.getUuid()
            ));
        }

        MacOperator mo = new MacOperator();
        mo.validateAvailableMac(msg.getMac());
        if (!VmNicState.disable.equals(vo.getState())) {
            if (mo.checkDuplicateMac(vmInstanceVO.getHypervisorType(), msg.getMac())) {
                throw new ApiMessageInterceptionException(Platform.argerr("Duplicate mac address [%s]", msg.getMac()));
            }
        }

        msg.setMac(msg.getMac().toLowerCase());
        msg.setVmInstanceUuid(vmInstanceVO.getUuid());
    }

    private void validate(APISetVmCleanTrafficMsg msg) {
        String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid()).select(VmInstanceVO_.type).findValue();
        if (!VmInstanceConstant.USER_VM_TYPE.equals(vmType)) {
            throw new ApiMessageInterceptionException(argerr(
                    "clean traffic is not supported for vm type [%s]", vmType)
            );
        }
    }

    private void validate(APIChangeVmImageMsg msg) {
        // check resource owner
        if (!acmgr.isAdmin(msg.getSession())
                && !acmgr.getOwnerAccountUuidOfResource(msg.getVmInstanceUuid()).equals(msg.getSession().getAccountUuid())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "user has no privilege to change image of vm %s",
                    msg.getVmInstanceUuid()
            ));
        }

        // vm must be stopped
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();
        if (state != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(argerr(
                    "do not change vm image when it's not stopped"
            ));
        }

        // primary storage must be Connected and Enabled
        String sql = "select ps" +
                " from PrimaryStorageVO ps, VolumeVO vol, VmInstanceVO vm" +
                " where vm.uuid = :vmUuid" +
                " and vm.rootVolumeUuid = vol.uuid" +
                " and vol.primaryStorageUuid = ps.uuid";
        TypedQuery<PrimaryStorageVO> query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
        query.setParameter("vmUuid", msg.getVmInstanceUuid());
        PrimaryStorageVO ps = query.getSingleResult();
        if (ps == null ||
                ps.getState() != PrimaryStorageState.Enabled ||
                ps.getStatus() != PrimaryStorageStatus.Connected) {
            throw new ApiMessageInterceptionException(argerr(
                    "make sure the primary storage vm[uuid:%s] was on is Enabled and Connected", msg.getVmInstanceUuid()
            ));
        }

        // if LocalStorage, vm.lastHost must be Enabled and Connected
        if (ps.getType().equals("LocalStorage")) {
            HostVO host = SQL.New("select host" +
                    " from HostVO host, VmInstanceVO vm" +
                    " where vm.uuid = :vmUuid" +
                    " and vm.lastHostUuid = host.uuid")
                    .param("vmUuid", msg.getVmInstanceUuid()).find();
            if (host == null ||
                    host.getState() != HostState.Enabled ||
                    host.getStatus() != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(argerr(
                        "make sure the last host vm[uuid:%s] was on is Enabled and Connected", msg.getVmInstanceUuid()
                ));
            }

            boolean withDefaultL3 = Q.New(VmInstanceVO.class)
                    .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .notNull(VmInstanceVO_.defaultL3NetworkUuid)
                    .isExists();
            if (!withDefaultL3) {
                throw new ApiMessageInterceptionException(argerr(
                        "vm[uuid:%s] has no default l3, cannot change image for it", msg.getVmInstanceUuid()
                ));
            }
        }

        // get image candidates
        GetImageCandidatesForVmToChangeMsg gmsg = new GetImageCandidatesForVmToChangeMsg();
        gmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(gmsg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        MessageReply r = bus.call(gmsg);
        if (r.isSuccess()) {
            GetImageCandidatesForVmToChangeReply rly = r.castReply();
            for (ImageInventory image : rly.getInventories()) {
                if (image.getUuid().equals(msg.getImageUuid())) {
                    return;
                }
            }
        }

        throw new ApiMessageInterceptionException(argerr(
                "instance[uuid:%s] cannot be changed image to image[uuid:%s]",
                msg.getVmInstanceUuid(), msg.getImageUuid()
        ));
    }

    private void validate(final APIChangeVmPasswordMsg msg) {
        if (msg.getVmInstanceUuid() == null || msg.getAccount() == null
                || msg.getPassword() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "either uuid or account or password must be set"
            ));
        }

        VmPasswordStrengthConfig vmPasswordStrengthConfig = VmPasswordStrengthConfig.toObject(MevocoGlobalConfig.VM_PASSWORD_STRENGTH_CHECK_CONFIG.value());
        if (vmPasswordStrengthConfig.isCheckPasswordStrength()) {
            vmPasswordStrengthConfig.validatePasswordStrengthConfig(msg.getPassword());
        }
    }

    private void validate(final APIDeleteNicQosMsg msg) {
        if (msg.getDirection().equals("in") && msg.getDirection().equals("out")){
            throw new ApiMessageInterceptionException(argerr("direction must be set in (in, out), but was %s", msg.getDirection()));
        }
    }

    private void validate(final APISetVmMonitorNumberMsg msg) {
        if (msg.getMonitorNumber() != 1 && msg.getMonitorNumber() != 2 && msg.getMonitorNumber()!= 4) {
            throw new ApiMessageInterceptionException(argerr(
                    "Monitor number must be 1 or 2 or 4."
            ));
        }
    }

    private void validate(final APISetNicQosMsg msg) {
        if (msg.getInboundBandwidth() == null && msg.getOutboundBandwidth() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "outboundBandwidth and inboundBandwidth must be set at lease one."
            ));
        }
    }

    private void validate(final APIGetNicQosMsg msg) {
        VmNicVO vvo = dbf.findByUuid(msg.getUuid(), VmNicVO.class);
        if (vvo == null) {
            throw new ApiMessageInterceptionException(argerr("nic id: %s does not exist...", msg.getUuid()));
        }
    }

    private void validate(final APIGetVirtualizerInfoMsg msg) {
        // 1. check msg.uuids : must be VmInstanceVO.uuid / HostVO.uuid
        long uuidCount = Q.New(ResourceVO.class)
                .in(ResourceVO_.uuid, msg.getUuids())
                .in(ResourceVO_.resourceType, asList(HostVO.class.getSimpleName(), VmInstanceVO.class.getSimpleName()))
                .count();
        if (uuidCount != msg.getUuids().size()) {
            throw new ApiMessageInterceptionException(argerr(
                    "The 'uuids' parameter must belong to the VmInstanceVO or HostVO"));
        }

        // 2. check VmInstanceVO / HostVO account
        if (!Account.isAdminPermission(msg.getSession())) {
            final String accountUuid = msg.getSession().getAccountUuid();
            long count = Q.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, msg.getUuids())
                    .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .count();
            if (count != msg.getUuids().size()) {
                throw new ApiMessageInterceptionException(argerr(
                        "resource[uuids:%s] is not owned by account[uuid:%s]", msg.getUuids(), accountUuid));
            }
        }
    }

    private void validate(APICreateTemplatedVmInstanceFromVmInstanceMsg msg) {
        ensureVmWithoutShareableVolume(msg.getVmInstanceUuid());

        if (msg.getHostUuid() == null) {
            Tuple tuple = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                    .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid, VmInstanceVO_.state).findTuple();

            VmInstanceState vmInstanceState = tuple.get(2, VmInstanceState.class);
            if (vmInstanceState == VmInstanceState.Running || vmInstanceState == VmInstanceState.Paused) {
                msg.setHostUuid(tuple.get(0, String.class));
            } else {
                msg.setHostUuid(tuple.get(1, String.class));
            }
        }
    }

    private void validate(APICreateVmInstanceFromTemplatedVmInstanceMsg msg) {
        VmInstanceVO templatedVmInstance = msg.getTemplatedVmInstance();

        TemplatedVmInstanceCacheVO cache = Q.New(TemplatedVmInstanceCacheVO.class)
                .eq(TemplatedVmInstanceCacheVO_.templatedVmInstanceUuid, msg.getTemplatedVmInstanceUuid()).find();
        if (cache != null) {
            List<VolumeSnapshotGroupVO> groups = Q.New(VolumeSnapshotGroupVO.class)
                    .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, cache.getCacheVmInstanceUuid()).list();
            if (groups.size() >= 2) {
                throw new ApiMessageInterceptionException(argerr("the cache of a templated vmInstance[uuid:%s] " +
                                "can contain only one or zero snapshot groups. the current number of snapshot groups is %d.",
                        msg.getTemplatedVmInstanceUuid(), groups.size()));
            }
        }

        if (msg.getZoneUuid() == null && msg.getClusterUuid() == null && msg.getHostUuid() == null) {
            msg.setClusterUuid(templatedVmInstance.getClusterUuid());
        }

        msg.setCpuNum(msg.getCpuNum() != null ? msg.getCpuNum() : templatedVmInstance.getCpuNum());
        msg.setMemorySize(msg.getMemorySize() != null ? msg.getMemorySize() : templatedVmInstance.getMemorySize());
        msg.setReservedMemorySize(msg.getReservedMemorySize() != null ? msg.getReservedMemorySize() : templatedVmInstance.getReservedMemorySize());
        msg.setType(templatedVmInstance.getType());
        msg.setDescription(msg.getDescription() != null ? msg.getDescription() : templatedVmInstance.getDescription());
        msg.setSystemTags(msg.getSystemTags() != null ? msg.getSystemTags() : new ArrayList<>());

        checkVmInstanceType(templatedVmInstance.getType());
        checkPrimaryStorageCapacity(msg.getNames().size(), templatedVmInstance);

        new VmInstanceHelper().validate(msg);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APIChangeVmPasswordMsg.class,
            APISetNicQosMsg.class,
            APISetVmMonitorNumberMsg.class,
            APIDeleteNicQosMsg.class,
            APIGetNicQosMsg.class,
            APIChangeVmImageMsg.class,
            APIUpdateVmNicMacMsg.class,
            APISetVmCleanTrafficMsg.class,
            APICloneVmInstanceMsg.class,
            APISetVmSecurityLevelMsg.class,
            APISetVmConsolePasswordMsg.class,
            APICreateVmInstanceMsg.class,
            APIGetVirtualizerInfoMsg.class,
            APICreateVmInstanceFromTemplatedVmInstanceMsg.class,
            APICreateTemplatedVmInstanceFromVmInstanceMsg.class,
            APIConvertVmInstanceToTemplatedVmInstanceMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
