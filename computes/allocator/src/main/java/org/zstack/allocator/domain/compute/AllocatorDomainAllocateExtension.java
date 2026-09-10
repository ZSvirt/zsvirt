package org.zstack.allocator.domain.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraint;
import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainConstraintResolver;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AllocateHostBeforeExtensionPoint;
import org.zstack.header.allocator.AllocateHostMsg;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostAllocateExtensionPoint;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkState;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmLocationSpec;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;

public class AllocatorDomainAllocateExtension implements AllocateHostBeforeExtensionPoint, HostAllocateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AllocatorDomainAllocateExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AllocatorDomainConstraintResolver constraintResolver;

    private final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Override
    public void beforeAllocateHost(AllocateHostMsg msg) {
        AllocatorDomainScene scene = msg.getAllocationScene();
        VmInstanceInventory vm = msg.getVmInstance();
        if (scene == null || vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return;
        }
        if (!VmInstanceConstant.USER_VM_TYPE.equals(vm.getType())) {
            return;
        }

        AllocatorDomainConstraint constraint = constraintResolver.resolve(vm.getUuid(), scene);
        if (constraint.kind == AllocatorDomainConstraint.Kind.ACTIVE) {
            putDomainRuleSet(msg, constraint.domain.getUuid());
            applyHostFilter(msg, constraint.domain);
            return;
        }
        if (!msg.isDryRun()
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

    private void putDomainRuleSet(AllocateHostMsg msg, String domainUuid) {
        msg.addRuleSet(AllocatorHostRuleSet.domain(domainUuid));
    }

    private void applyHostFilter(AllocateHostMsg msg, AllocatorDomainVO domain) {
        String domainZoneUuid = domain.getZoneUuid();
        logger.debug(String.format("allocator domain[uuid:%s, zone:%s] constrains host allocation for vm[uuid:%s] scene[%s]",
                domain.getUuid(), domainZoneUuid, msg.getVmInstance().getUuid(), msg.getAllocationScene()));

        List<HostVO> candidates = Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, domainZoneUuid)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(
                domain.getRules(), candidates, domainZoneUuid);
        applyPrimaryStorageHostFilter(msg, domain, candidates, decision);
        applyL3NetworkHostFilter(msg, domain, candidates, decision);
        rejectIfNoAllowedHost(domain.getUuid(), decision);

        msg.addLocationSpec(VmLocationSpec.onlyAllowedHost(new ArrayList<>(decision.allowedHostUuids)));
        if (!decision.recommendedHostUuids.isEmpty()) {
            msg.addLocationSpec(VmLocationSpec.recommendHost(new ArrayList<>(decision.recommendedHostUuids)));
        }
        if (!decision.notRecommendedHostUuids.isEmpty()) {
            msg.addLocationSpec(VmLocationSpec.notRecommendHost(new ArrayList<>(decision.notRecommendedHostUuids)));
        }
    }

    private void applyPrimaryStorageHostFilter(AllocateHostMsg msg, AllocatorDomainVO domain, List<HostVO> candidates,
                                               AllocatorDomainRuleEvaluator.HostDecision decision) {
        List<String> zonePrimaryStorages = listZonePrimaryStorageUuids(domain.getZoneUuid());
        AllocatorDomainRuleEvaluator.UuidDecision psDecision = evaluator.evaluatePrimaryStorages(
                domain.getRules(), zonePrimaryStorages);
        if (!psDecision.hasChain) {
            return;
        }

        Set<String> required = msg.getRequiredPrimaryStorageUuids() == null
                ? Collections.emptySet() : msg.getRequiredPrimaryStorageUuids();
        for (String psUuid : required) {
            if (!psDecision.allowedUuids.contains(psUuid)) {
                throw err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "primary storage[uuid:%s] is not in allocator domain[uuid:%s]", psUuid, domain.getUuid())
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domain.getUuid())
                        .withOpaque(AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, psUuid)
                        .toException();
            }
        }

        Set<String> allowedClusters = required.isEmpty()
                ? clustersAttachedToAny(psDecision.allowedUuids)
                : clustersAttachedToAllPrimaryStorages(required);
        retainHostsByClusters(decision, candidates, allowedClusters);
    }

    private void applyL3NetworkHostFilter(AllocateHostMsg msg, AllocatorDomainVO domain, List<HostVO> candidates,
                                          AllocatorDomainRuleEvaluator.HostDecision decision) {
        List<String> zoneL3s = listZoneL3NetworkUuids(domain.getZoneUuid());
        AllocatorDomainRuleEvaluator.UuidDecision l3Decision = evaluator.evaluateL3Networks(domain.getRules(), zoneL3s);
        if (!l3Decision.hasChain) {
            return;
        }

        List<String> specified = msg.getL3NetworkUuids();
        if (specified == null || specified.isEmpty()) {
            return;
        }

        for (String l3Uuid : specified) {
            if (!l3Decision.allowedUuids.contains(l3Uuid)) {
                throw err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                        "l3 network[uuid:%s] is not in allocator domain[uuid:%s]", l3Uuid, domain.getUuid())
                        .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domain.getUuid())
                        .withOpaque(AllocatorDomainConstant.Opaque.L3_UUID, l3Uuid)
                        .toException();
            }
        }

        retainHostsByClusters(decision, candidates, clustersAttachedToAllL3Networks(specified));
    }

    private void rejectIfNoAllowedHost(String domainUuid, AllocatorDomainRuleEvaluator.HostDecision decision) {
        if (!decision.allowedHostUuids.isEmpty()) {
            return;
        }
        throw err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                "no host in allocator domain[uuid:%s] for this allocation", domainUuid)
                .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domainUuid)
                .toException();
    }

    private void retainHostsByClusters(AllocatorDomainRuleEvaluator.HostDecision decision, List<HostVO> candidates,
                                       Set<String> clusterUuids) {
        Set<String> keep = new LinkedHashSet<>();
        List<HostVO> hosts = candidates == null ? Collections.emptyList() : candidates;
        Set<String> clusters = clusterUuids == null ? Collections.emptySet() : clusterUuids;
        for (HostVO host : hosts) {
            if (!decision.allowedHostUuids.contains(host.getUuid())) {
                continue;
            }
            if (host.getClusterUuid() != null && clusters.contains(host.getClusterUuid())) {
                keep.add(host.getUuid());
            }
        }
        decision.allowedHostUuids.retainAll(keep);
        decision.recommendedHostUuids.retainAll(keep);
        decision.notRecommendedHostUuids.retainAll(keep);
    }

    private List<String> listZonePrimaryStorageUuids(String zoneUuid) {
        List<String> uuids = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .select(PrimaryStorageVO_.uuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private List<String> listZoneL3NetworkUuids(String zoneUuid) {
        List<String> uuids = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.zoneUuid, zoneUuid)
                .eq(L3NetworkVO_.state, L3NetworkState.Enabled)
                .select(L3NetworkVO_.uuid)
                .listValues();
        return uuids == null ? new ArrayList<>() : uuids;
    }

    private Set<String> clustersAttachedToAllPrimaryStorages(Collection<String> psUuids) {
        Set<String> clusters = null;
        for (String psUuid : psUuids) {
            List<String> attached = listClusterUuidsByPrimaryStorage(psUuid);
            if (clusters == null) {
                clusters = new LinkedHashSet<>(attached);
            } else {
                clusters.retainAll(attached);
            }
        }
        return clusters == null ? Collections.emptySet() : clusters;
    }

    private Set<String> clustersAttachedToAny(Collection<String> psUuids) {
        if (psUuids == null || psUuids.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> values = Q.New(PrimaryStorageClusterRefVO.class)
                .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuids)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? Collections.emptySet() : new LinkedHashSet<>(values);
    }

    private List<String> listClusterUuidsByPrimaryStorage(String psUuid) {
        List<String> values = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? new ArrayList<>() : values;
    }

    private Set<String> clustersAttachedToAllL3Networks(Collection<String> l3Uuids) {
        Set<String> clusters = null;
        for (String l3Uuid : l3Uuids) {
            String l2Uuid = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .select(L3NetworkVO_.l2NetworkUuid)
                    .findValue();
            if (l2Uuid == null) {
                return Collections.emptySet();
            }
            List<String> attached = listClusterUuidsByL2(l2Uuid);
            if (clusters == null) {
                clusters = new LinkedHashSet<>(attached);
            } else {
                clusters.retainAll(attached);
            }
        }
        return clusters == null ? Collections.emptySet() : clusters;
    }

    private List<String> listClusterUuidsByL2(String l2Uuid) {
        List<String> values = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2Uuid)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .listValues();
        return values == null ? new ArrayList<>() : values;
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
