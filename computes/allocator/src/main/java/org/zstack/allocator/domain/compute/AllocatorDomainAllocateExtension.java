package org.zstack.allocator.domain.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraint;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraintResolver;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.header.allocator.domain.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AllocateHostBeforeExtensionPoint;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostAllocateExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;
import java.util.List;

public class AllocatorDomainAllocateExtension implements AllocateHostBeforeExtensionPoint, HostAllocateExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AllocatorDomainConstraintResolver constraintResolver;

    @Override
    public void beforeAllocateHost(HostAllocatorSpec spec) {
        AllocatorDomainScene scene = spec.getAllocationScene();
        VmInstanceInventory vm = spec.getVmInstance();
        if (scene == null || vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return;
        }
        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            return;
        }

        AllocatorDomainConstraint constraint = constraintResolver.resolve(vm.getUuid(), scene);
        if (constraint.kind == AllocatorDomainConstraint.Kind.ACTIVE) {
            spec.addRuleSet(AllocatorDomains.transform(constraint.domain));
            return;
        }
        if (!spec.isDryRun()
                && (scene == AllocatorDomainScene.NewCreate || scene == AllocatorDomainScene.Start)
                && (constraint.kind == AllocatorDomainConstraint.Kind.NONE
                || constraint.kind == AllocatorDomainConstraint.Kind.SCENE_NOT_IN_TABLE)) {
            deleteSchedulingDomainTag(vm.getUuid());
        }
    }

    @Override
    public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
        if (spec.isDryRun()) {
            return;
        }
        AllocatorDomainScene scene = spec.getAllocationScene();
        VmInstanceInventory vm = spec.getVmInstance();
        if (vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return;
        }
        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            return;
        }

        if (scene == null) {
            return;
        }
        switch (scene) {
            case NewCreate: case Start:
                String domainUuid = domainUuidFromRuleSets(spec.getRuleSets());
                if (domainUuid != null) {
                    writeSchedulingDomainTag(vm.getUuid(), domainUuid);
                }
                break;
            case Migrate: case Ha: case HostMaintenance: case Drs:
                applyRunningSuccess(spec, vm.getUuid());
                break;
            default:
                break;
        }
    }

    private void applyRunningSuccess(HostAllocatorSpec spec, String vmUuid) {
        String domainUuid = domainUuidFromRuleSets(spec.getRuleSets());
        if (domainUuid != null) {
            writeSchedulingDomainTag(vmUuid, domainUuid);
            return;
        }

        String tagUuid = constraintResolver.readTagDomainUuid(vmUuid);
        if (tagUuid == null) {
            return;
        }

        AllocatorDomainVO domain = dbf.findByUuid(tagUuid, AllocatorDomainVO.class);
        if (domain == null) {
            deleteSchedulingDomainTag(vmUuid);
            return;
        }
        if (domain.getState() == AllocatorDomainState.Disabled) {
            return;
        }
        if (!constraintResolver.sceneInTable(domain, spec.getAllocationScene())) {
            deleteSchedulingDomainTag(vmUuid);
        }
    }

    private String domainUuidFromRuleSets(List<AllocatorHostRuleSet> ruleSets) {
        if (ruleSets == null) {
            return null;
        }
        for (AllocatorHostRuleSet set : ruleSets) {
            if (set != null && AllocatorHostRuleSet.NAME_DOMAIN.equals(set.name)
                    && !StringUtils.isEmpty(set.domainUuid)) {
                return set.domainUuid;
            }
        }
        return null;
    }

    private void writeSchedulingDomainTag(String vmUuid, String domainUuid) {
        SystemTagCreator creator = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.newSystemTagCreator(vmUuid);
        creator.recreate = true;
        creator.setTagByTokens(Collections.singletonMap(AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN, domainUuid));
        creator.create();
    }

    private void deleteSchedulingDomainTag(String vmUuid) {
        AllocatorDomainSystemTags.SCHEDULING_DOMAIN.delete(vmUuid);
    }
}
