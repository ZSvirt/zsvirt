package org.zstack.allocator.domain.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraint;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraintResolver;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig;
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.tag.TagConstant;
import org.zstack.header.vm.APIAttachL3NetworkToVmMsg;
import org.zstack.header.vm.APIAttachVmNicToVmMsg;
import org.zstack.header.vm.APIChangeVmNicNetworkMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.APIStartVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.volume.APIAttachDataVolumeToVmMsg;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Tags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Specified host / cluster / PS / L3 out of the VM's allocator domain → 4001.
 * Create VM carrying current-value tag {@code schedulingDomain::} → 4002.
 */
public class AllocatorDomainVmApiInterceptor implements GlobalApiMessageInterceptor {
    private static final String RESOURCE_CONFIG_DOMAIN_PREFIX = Tags.prefix(
            TagConstant.RESOURCE_CONFIG_TAG_PREFIX,
            AllocatorDomainGlobalConfig.CATEGORY,
            AllocatorDomainGlobalConfig.DOMAIN_UUID_NAME
    );

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AllocatorDomainConstraintResolver constraintResolver;

    private final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeMsg) {
            validate((APICreateVmInstanceFromVolumeMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            validate((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIAttachVmNicToVmMsg) {
            validate((APIAttachVmNicToVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicNetworkMsg) {
            validate((APIChangeVmNicNetworkMsg) msg);
        } else if (msg instanceof APIAttachDataVolumeToVmMsg) {
            validate((APIAttachDataVolumeToVmMsg) msg);
        }
        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
                APICreateVmInstanceMsg.class,
                APICreateVmInstanceFromVolumeMsg.class,
                APICreateVmInstanceFromVolumeSnapshotMsg.class,
                APICreateVmInstanceFromVolumeSnapshotGroupMsg.class,
                APIStartVmInstanceMsg.class,
                APIMigrateVmMsg.class,
                APIAttachL3NetworkToVmMsg.class,
                APIAttachVmNicToVmMsg.class,
                APIChangeVmNicNetworkMsg.class,
                APIAttachDataVolumeToVmMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private void validate(APICreateVmInstanceMsg msg) {
        validateCreate(msg, specifiedPrimaryStorages(msg));
    }

    private void validate(APICreateVmInstanceFromVolumeMsg msg) {
        String psUuid = msg.getPrimaryStorageUuid();
        if (StringUtils.isEmpty(psUuid)) {
            psUuid = primaryStorageUuidOfVolume(msg.getVolumeUuid());
        }
        validateCreate(msg, singletonOrEmpty(psUuid));
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        String psUuid = msg.getPrimaryStorageUuid();
        if (StringUtils.isEmpty(psUuid)) {
            psUuid = primaryStorageUuidOfSnapshot(msg.getVolumeSnapshotUuid());
        }
        validateCreate(msg, singletonOrEmpty(psUuid));
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        String psUuid = msg.getPrimaryStorageUuidForRootVolume();
        if (StringUtils.isEmpty(psUuid)) {
            psUuid = primaryStorageUuidOfSnapshot(rootSnapshotUuidOfGroup(msg.getVolumeSnapshotGroupUuid()));
        }
        validateCreate(msg, singletonOrEmpty(psUuid));
    }

    private void validateCreate(NewVmInstanceMessage2 msg, Collection<String> specifiedPrimaryStorageUuids) {
        if (!isUserVmType(msg.getType())) {
            return;
        }
        rejectCurrentValueTag(msg.getSystemTags());
        AllocatorHostRuleSet set = activeSet(constraintResolver.resolveByDomainUuid(
                readCreateConfigDomainUuid(msg.getSystemTags()), AllocatorDomainScene.NewCreate));
        if (set == null) {
            return;
        }
        checkHostAndCluster(set, msg.getHostUuid(), msg.getClusterUuid());
        checkPrimaryStorages(set, specifiedPrimaryStorageUuids);
        checkL3Networks(set, specifiedL3Uuids(msg));
    }

    private void validate(APIStartVmInstanceMsg msg) {
        if (!isUserVmInstance(msg.getUuid())) {
            return;
        }
        AllocatorHostRuleSet set = activeSet(constraintResolver.resolve(msg.getUuid(), AllocatorDomainScene.Start));
        if (set == null) {
            return;
        }
        checkHostAndCluster(set, msg.getHostUuid(), msg.getClusterUuid());
    }

    private void validate(APIMigrateVmMsg msg) {
        if (!isUserVmInstance(msg.getVmInstanceUuid())) {
            return;
        }
        AllocatorHostRuleSet set = activeSet(constraintResolver.resolve(msg.getVmInstanceUuid(), AllocatorDomainScene.Migrate));
        if (set == null) {
            return;
        }
        checkHostAndCluster(set, msg.getHostUuid(), null);
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        AllocatorHostRuleSet set = activeAttachSet(msg.getVmInstanceUuid());
        if (set == null) {
            return;
        }
        checkL3Networks(set, singletonOrEmpty(msg.getL3NetworkUuid()));
    }

    private void validate(APIAttachVmNicToVmMsg msg) {
        AllocatorHostRuleSet set = activeAttachSet(msg.getVmInstanceUuid());
        if (set == null) {
            return;
        }
        String l3Uuid = Q.New(VmNicVO.class)
                .eq(VmNicVO_.uuid, msg.getVmNicUuid())
                .select(VmNicVO_.l3NetworkUuid)
                .findValue();
        checkL3Networks(set, singletonOrEmpty(l3Uuid));
    }

    private void validate(APIChangeVmNicNetworkMsg msg) {
        AllocatorHostRuleSet set = activeAttachSet(msg.getVmInstanceUuid());
        if (set == null) {
            return;
        }
        checkL3Networks(set, singletonOrEmpty(msg.getDestL3NetworkUuid()));
    }

    private void validate(APIAttachDataVolumeToVmMsg msg) {
        AllocatorHostRuleSet set = activeAttachSet(msg.getVmInstanceUuid());
        if (set == null) {
            return;
        }
        checkPrimaryStorages(set, singletonOrEmpty(primaryStorageUuidOfVolume(msg.getVolumeUuid())));
    }

    private AllocatorHostRuleSet activeAttachSet(String vmUuid) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (!isUserVmType(vm.getType())) {
            return null;
        }
        String domainUuid = vm.getState() == VmInstanceState.Running
                ? constraintResolver.readTagDomainUuid(vmUuid)
                : constraintResolver.readConfigDomainUuid(vmUuid);
        return activeSet(constraintResolver.resolveByDomainUuid(domainUuid, null));
    }

    private AllocatorHostRuleSet activeSet(AllocatorDomainConstraint constraint) {
        if (constraint == null || constraint.kind != AllocatorDomainConstraint.Kind.ACTIVE) {
            return null;
        }
        return AllocatorDomains.transform(constraint.domain);
    }

    private void checkHostAndCluster(AllocatorHostRuleSet set, String hostUuid, String clusterUuid) {
        if (!evaluator.hasHostChain(set.rules)) {
            return;
        }
        List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, set.zoneUuid)
                .list();
        if (hosts == null) {
            hosts = new ArrayList<>();
        }
        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(set.rules, hosts, set.zoneUuid);
        if (!StringUtils.isEmpty(hostUuid) && !decision.allowedHostUuids.contains(hostUuid)) {
            failOutOfDomain(set.domainUuid, AllocatorDomainConstant.Opaque.HOST_UUID, hostUuid,
                    "host[uuid:%s] is not in allocator domain[uuid:%s]", hostUuid, set.domainUuid);
        }
        if (StringUtils.isEmpty(clusterUuid)) {
            return;
        }
        boolean clusterAllowed = false;
        for (HostVO host : hosts) {
            if (clusterUuid.equals(host.getClusterUuid())
                    && decision.allowedHostUuids.contains(host.getUuid())) {
                clusterAllowed = true;
                break;
            }
        }
        if (!clusterAllowed) {
            failOutOfDomain(set.domainUuid, AllocatorDomainConstant.Opaque.CLUSTER_UUID, clusterUuid,
                    "cluster[uuid:%s] is not in allocator domain[uuid:%s]", clusterUuid, set.domainUuid);
        }
    }

    private void checkPrimaryStorages(AllocatorHostRuleSet set, Collection<String> specified) {
        List<String> uuids = nonEmpty(specified);
        if (uuids.isEmpty()) {
            return;
        }
        AllocatorDomainRuleEvaluator.UuidDecision decision = evaluator.evaluatePrimaryStorages(set.rules, uuids);
        if (!decision.hasChain) {
            return;
        }
        for (String uuid : uuids) {
            if (!decision.allowedUuids.contains(uuid)) {
                failOutOfDomain(set.domainUuid, AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, uuid,
                        "primary storage[uuid:%s] is not in allocator domain[uuid:%s]", uuid, set.domainUuid);
            }
        }
    }

    private void checkL3Networks(AllocatorHostRuleSet set, Collection<String> specified) {
        List<String> uuids = nonEmpty(specified);
        if (uuids.isEmpty()) {
            return;
        }
        AllocatorDomainRuleEvaluator.UuidDecision decision = evaluator.evaluateL3Networks(set.rules, uuids);
        if (!decision.hasChain) {
            return;
        }
        for (String uuid : uuids) {
            if (!decision.allowedUuids.contains(uuid)) {
                failOutOfDomain(set.domainUuid, AllocatorDomainConstant.Opaque.L3_UUID, uuid,
                        "l3 network[uuid:%s] is not in allocator domain[uuid:%s]", uuid, set.domainUuid);
            }
        }
    }

    private void rejectCurrentValueTag(List<String> systemTags) {
        if (systemTags == null) {
            return;
        }
        for (String tag : systemTags) {
            if (tag != null && AllocatorDomainSystemTags.SCHEDULING_DOMAIN.isMatch(tag)) {
                throw new ApiMessageInterceptionException(err(AllocatorDomainErrors.DOMAIN_UUID_CHANGE_NOT_ALLOWED,
                        "creating a vm with current-value system tag schedulingDomain is not allowed"));
            }
        }
    }

    private String readCreateConfigDomainUuid(List<String> systemTags) {
        if (systemTags == null) {
            return null;
        }
        for (String tag : systemTags) {
            if (tag == null || !tag.startsWith(RESOURCE_CONFIG_DOMAIN_PREFIX)) {
                continue;
            }
            String value = tag.substring(RESOURCE_CONFIG_DOMAIN_PREFIX.length());
            if (StringUtils.isEmpty(value) || AllocatorDomainConstant.UNBOUND_DOMAIN_UUID.equals(value)) {
                return null;
            }
            return value;
        }
        return null;
    }

    private List<String> specifiedPrimaryStorages(APICreateVmInstanceMsg msg) {
        Set<String> uuids = new LinkedHashSet<>();
        if (msg.getDiskAOs() == null) {
            return new ArrayList<>();
        }
        for (DiskAO disk : msg.getDiskAOs()) {
            if (disk == null) {
                continue;
            }
            if (!StringUtils.isEmpty(disk.getPrimaryStorageUuid())) {
                uuids.add(disk.getPrimaryStorageUuid());
            }
            if (VolumeVO.class.getSimpleName().equals(disk.getSourceType())
                    && !StringUtils.isEmpty(disk.getSourceUuid())) {
                String psUuid = primaryStorageUuidOfVolume(disk.getSourceUuid());
                if (!StringUtils.isEmpty(psUuid)) {
                    uuids.add(psUuid);
                }
            }
        }
        return new ArrayList<>(uuids);
    }

    private List<String> specifiedL3Uuids(NewVmInstanceMessage2 msg) {
        Set<String> uuids = new LinkedHashSet<>();
        if (msg.getL3NetworkUuids() != null) {
            for (String uuid : msg.getL3NetworkUuids()) {
                if (!StringUtils.isEmpty(uuid)) {
                    uuids.add(uuid);
                }
            }
        }
        if (!StringUtils.isEmpty(msg.getDefaultL3NetworkUuid())) {
            uuids.add(msg.getDefaultL3NetworkUuid());
        }
        return new ArrayList<>(uuids);
    }

    private String primaryStorageUuidOfVolume(String volumeUuid) {
        if (StringUtils.isEmpty(volumeUuid)) {
            return null;
        }
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.primaryStorageUuid)
                .findValue();
    }

    private String primaryStorageUuidOfSnapshot(String snapshotUuid) {
        if (StringUtils.isEmpty(snapshotUuid)) {
            return null;
        }
        return Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .select(VolumeSnapshotVO_.primaryStorageUuid)
                .findValue();
    }

    private String rootSnapshotUuidOfGroup(String groupUuid) {
        if (StringUtils.isEmpty(groupUuid)) {
            return null;
        }
        return Q.New(VolumeSnapshotGroupRefVO.class)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeType, VolumeType.Root.toString())
                .select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid)
                .findValue();
    }

    private static boolean isUserVmInstance(String vmUuid) {
        String type = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.type)
                .findValue();
        return isUserVmType(type);
    }

    private static boolean isUserVmType(String type) {
        return type == null || VmInstanceConstant.USER_VM_TYPE.equals(type);
    }

    private List<String> singletonOrEmpty(String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(uuid);
    }

    private List<String> nonEmpty(Collection<String> specified) {
        if (specified == null || specified.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> uuids = new ArrayList<>();
        for (String uuid : specified) {
            if (!StringUtils.isEmpty(uuid)) {
                uuids.add(uuid);
            }
        }
        return uuids;
    }

    private void failOutOfDomain(String domainUuid, String resourceOpaqueKey, String resourceUuid,
                                 String format, Object... args) {
        throw new ApiMessageInterceptionException(err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN, format, args)
                .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domainUuid)
                .withOpaque(resourceOpaqueKey, resourceUuid));
    }
}
