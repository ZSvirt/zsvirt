package org.zstack.allocator.domain.compute.evaluator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.compute.AllocatorDomains;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeType;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Create-time PS membership for a volume type. Root is wired from
 * {@code DomainFilterPrimaryStorageFlow}. Data is not: product diskAO extra disks
 * allocate in OtherDisk / attach, not in {@code VmAllocatePrimaryStorageFlow}.
 */
public class AllocatorDomainPrimaryStorageGate {
    private final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Autowired
    private AllocatorDomainConstraintResolver constraintResolver;

    public ErrorCode filterRootVolume(VmInstanceSpec spec) {
        if (spec == null) {
            return null;
        }
        VmInstanceInventory vm = spec.getVmInventory();
        if (vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return null;
        }
        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            return null;
        }

        AllocatorDomainConstraint constraint = constraintResolver.resolve(vm.getUuid(), AllocatorDomainScene.NewCreate);
        if (constraint.kind != AllocatorDomainConstraint.Kind.ACTIVE) {
            return null;
        }

        AllocatorHostRuleSet set = AllocatorDomains.transform(constraint.domain);
        if (set == null) {
            return null;
        }

        DiskAO root = spec.getRootDisk();
        String requiredPsUuid = root == null ? null : root.getPrimaryStorageUuid();
        List<String> universe = universeForUnspecified(spec, requiredPsUuid);
        AllocatorDomainRuleEvaluator.UuidDecision decision = evaluator.evaluatePrimaryStorages(
                set.rules, universe, VolumeType.Root.toString());
        if (!decision.hasChain) {
            return null;
        }

        if (!StringUtils.isEmpty(requiredPsUuid)) {
            if (!decision.allowedUuids.contains(requiredPsUuid)) {
                return err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "primary storage[uuid:%s] is not in allocator domain[uuid:%s]",
                        requiredPsUuid, set.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, set.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, requiredPsUuid);
            }
            return null;
        }

        List<String> filtered = new ArrayList<>();
        for (String uuid : universe) {
            if (uuid != null && decision.allowedUuids.contains(uuid)) {
                filtered.add(uuid);
            }
        }
        if (filtered.isEmpty()) {
            return err(AllocatorDomainErrors.NO_AVAILABLE_RESOURCE_IN_DOMAIN,
                    "cannot find primary storage in allocator domain[uuid:%s] for vm[uuid:%s] root volume",
                    set.domainUuid, vm.getUuid())
                    .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, set.domainUuid);
        }
        spec.setCandidatePrimaryStorageUuidsForRootVolume(filtered);
        return null;
    }

    /**
     * TODO: constrain data-volume PS with {@link VolumeType#Data} qualifier.
     * Call from OtherDisk / attach / deprecated allocate, not from DomainFilterPrimaryStorageFlow.
     */
    public ErrorCode filterDataVolume(VmInstanceSpec spec) {
        return null;
    }

    private List<String> universeForUnspecified(VmInstanceSpec spec, String requiredPsUuid) {
        if (!StringUtils.isEmpty(requiredPsUuid)) {
            List<String> specified = new ArrayList<>();
            specified.add(requiredPsUuid);
            return specified;
        }
        List<String> candidates = spec.getCandidatePrimaryStorageUuidsForRootVolume();
        if (!isEmpty(candidates)) {
            return new ArrayList<>(candidates);
        }
        HostInventory destHost = spec.getDestHost();
        if (destHost == null || StringUtils.isEmpty(destHost.getClusterUuid())) {
            return new ArrayList<>();
        }
        List<String> attached = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, destHost.getClusterUuid())
                .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                .listValues();
        return attached == null ? new ArrayList<>() : new ArrayList<>(attached);
    }
}
