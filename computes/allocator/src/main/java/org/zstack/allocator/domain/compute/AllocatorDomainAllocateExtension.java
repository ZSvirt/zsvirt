package org.zstack.allocator.domain.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorHostRuleSet;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig;
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
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmLocationSpec;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.err;

public class AllocatorDomainAllocateExtension implements AllocateHostBeforeExtensionPoint, HostAllocateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(AllocatorDomainAllocateExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    private final AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator();

    @Override
    public void beforeAllocateHost(AllocateHostMsg msg) {
        AllocatorDomainScene scene = msg.getAllocationScene();
        if (scene != AllocatorDomainScene.NewCreate && scene != AllocatorDomainScene.Start) {
            return;
        }

        VmInstanceInventory vm = msg.getVmInstance();
        if (vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return;
        }

        String configUuid = readConfigDomainUuid(vm.getUuid());
        if (msg.isDryRun()) {
            applyDryRun(msg, scene, configUuid);
            return;
        }

        applyNonDryRun(msg, scene, vm.getUuid(), configUuid);
    }

    private void applyDryRun(AllocateHostMsg msg, AllocatorDomainScene scene, String configUuid) {
        if (configUuid == null) {
            return;
        }

        AllocatorDomainVO domain = requireDomain(configUuid);
        if (domain.getState() == AllocatorDomainState.Disabled || !sceneInTable(domain, scene)) {
            return;
        }

        putDomainRuleSet(msg, configUuid);
        applyHostFilter(msg, domain);
    }

    private void applyNonDryRun(AllocateHostMsg msg, AllocatorDomainScene scene, String vmUuid, String configUuid) {
        if (configUuid == null) {
            deleteSchedulingDomainTag(vmUuid);
            return;
        }

        AllocatorDomainVO domain = requireDomain(configUuid);
        if (domain.getState() == AllocatorDomainState.Disabled) {
            return;
        }

        if (!sceneInTable(domain, scene)) {
            deleteSchedulingDomainTag(vmUuid);
            return;
        }

        putDomainRuleSet(msg, configUuid);
        applyHostFilter(msg, domain);
    }

    @Override
    public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
        if (spec.isDryRun()) {
            return;
        }
        AllocatorDomainScene scene = spec.getAllocationScene();
        if (scene != AllocatorDomainScene.NewCreate && scene != AllocatorDomainScene.Start) {
            return;
        }
        VmInstanceInventory vm = spec.getVmInstance();
        if (vm == null || StringUtils.isEmpty(vm.getUuid())) {
            return;
        }
        String domainUuid = domainUuidFromRuleSets(spec.getRuleSets());
        if (domainUuid == null) {
            return;
        }
        writeSchedulingDomainTag(vm.getUuid(), domainUuid);
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
        if (decision.allowedHostUuids.isEmpty()) {
            throw err(AllocatorDomainErrors.RESOURCE_OUT_OF_DOMAIN,
                    "no host in allocator domain[uuid:%s] for this allocation", domain.getUuid())
                    .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domain.getUuid())
                    .toException();
        }

        msg.addLocationSpec(VmLocationSpec.onlyAllowedHost(new ArrayList<>(decision.allowedHostUuids)));
        if (!decision.recommendedHostUuids.isEmpty()) {
            msg.addLocationSpec(VmLocationSpec.recommendHost(new ArrayList<>(decision.recommendedHostUuids)));
        }
        if (!decision.notRecommendedHostUuids.isEmpty()) {
            msg.addLocationSpec(VmLocationSpec.notRecommendHost(new ArrayList<>(decision.notRecommendedHostUuids)));
        }
    }

    private AllocatorDomainVO requireDomain(String domainUuid) {
        AllocatorDomainVO domain = dbf.findByUuid(domainUuid, AllocatorDomainVO.class);
        if (domain == null) {
            throw err(AllocatorDomainErrors.DOMAIN_NOT_FOUND,
                    "allocator domain[uuid:%s] not found", domainUuid)
                    .withOpaque(AllocatorDomainConstant.Opaque.DOMAIN_UUID, domainUuid)
                    .toException();
        }
        return domain;
    }

    private boolean sceneInTable(AllocatorDomainVO domain, AllocatorDomainScene scene) {
        if (domain.getScenes() == null) {
            return false;
        }
        return domain.getScenes().stream().anyMatch(s -> s.getScene() == scene);
    }

    private String readConfigDomainUuid(String vmUuid) {
        String value = rcf.getResourceConfigValue(AllocatorDomainGlobalConfig.SCHEDULING_DOMAIN_UUID, vmUuid, String.class);
        if (StringUtils.isEmpty(value) || AllocatorDomainConstant.UNBOUND_DOMAIN_UUID.equals(value)) {
            return null;
        }
        return value;
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
