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
import org.zstack.header.storage.primary.PrimaryStorageAllocationPurpose;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeType;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;

/**
 * PS membership for a volume type, applied on the primary-storage allocator candidate list.
 * Root is wired from {@code DomainFilterPrimaryStorageFlow} in allocatorFlowNames.
 */
public class AllocatorDomainPrimaryStorageGate {
    private final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Autowired
    private AllocatorDomainConstraintResolver constraintResolver;

    public ErrorCode filterRootVolume(PrimaryStorageAllocationSpec spec, List<PrimaryStorageVO> candidates) {
        if (spec == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (!PrimaryStorageAllocationPurpose.CreateNewVm.toString().equals(spec.getPurpose())) {
            return null;
        }
        String vmUuid = spec.getVmInstanceUuid();
        if (StringUtils.isEmpty(vmUuid) || !isUserVm(vmUuid)) {
            return null;
        }

        AllocatorDomainConstraint constraint = constraintResolver.resolve(vmUuid, AllocatorDomainScene.NewCreate);
        if (constraint.kind != AllocatorDomainConstraint.Kind.ACTIVE) {
            return null;
        }

        AllocatorHostRuleSet set = AllocatorDomains.transform(constraint.domain);
        if (set == null) {
            return null;
        }

        List<String> universe = new ArrayList<>();
        for (PrimaryStorageVO ps : candidates) {
            if (ps != null && ps.getUuid() != null) {
                universe.add(ps.getUuid());
            }
        }
        AllocatorDomainRuleEvaluator.UuidDecision decision = evaluator.evaluatePrimaryStorages(
                set.rules, universe, VolumeType.Root.toString());
        if (!decision.hasChain) {
            return null;
        }

        List<String> specified = spec.getCandidatePrimaryStorageUuids();
        if (specified.size() == 1) {
            String requiredPsUuid = specified.get(0);
            if (!decision.allowedUuids.contains(requiredPsUuid)) {
                return err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "primary storage[uuid:%s] is not in allocator domain[uuid:%s]",
                        requiredPsUuid, set.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, set.domainUuid)
                        .withOpaque(AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, requiredPsUuid);
            }
            return null;
        }

        candidates.removeIf(ps -> ps == null || ps.getUuid() == null || !decision.allowedUuids.contains(ps.getUuid()));
        if (candidates.isEmpty()) {
            return err(AllocatorDomainErrors.NO_AVAILABLE_RESOURCE_IN_DOMAIN,
                    "cannot find primary storage in allocator domain[uuid:%s] for vm[uuid:%s] root volume",
                    set.domainUuid, vmUuid)
                    .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, set.domainUuid);
        }
        return null;
    }

    /**
     * TODO: constrain data-volume PS with {@link VolumeType#Data} qualifier in the same
     * primary-storage allocatorFlowNames chain (CreateDataVolume / attach).
     */
    public ErrorCode filterDataVolume(VmInstanceSpec spec) {
        return null;
    }

    private boolean isUserVm(String vmUuid) {
        String type = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.type)
                .findValue();
        return VmInstanceConstant.USER_VM_TYPE.equals(type);
    }
}
