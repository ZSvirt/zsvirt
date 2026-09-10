package org.zstack.allocator.domain.compute.evaluator;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainGlobalConfig;
import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.allocator.domain.header.AllocatorDomainSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.resourceconfig.ResourceConfigFacade;

import static org.zstack.core.Platform.err;

/**
 * Resolves which allocator domain constrains this allocation.
 * Create/Start read ResourceConfig; running scenes read the current-value tag only.
 */
public class AllocatorDomainConstraintResolver {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;

    public AllocatorDomainConstraint resolve(String vmUuid, AllocatorDomainScene scene) {
        String domainUuid = readConstraintUuid(vmUuid, scene);
        if (domainUuid == null) {
            return AllocatorDomainConstraint.none();
        }

        AllocatorDomainVO domain = requireDomain(domainUuid);
        if (domain.getState() == AllocatorDomainState.Disabled) {
            return AllocatorDomainConstraint.disabled(domain);
        }
        if (!sceneInTable(domain, scene)) {
            return AllocatorDomainConstraint.sceneNotInTable(domain);
        }
        return AllocatorDomainConstraint.active(domain);
    }

    public String readTagDomainUuid(String vmUuid) {
        String value = AllocatorDomainSystemTags.SCHEDULING_DOMAIN.getTokenByResourceUuid(
                vmUuid, AllocatorDomainSystemTags.DOMAIN_UUID_TOKEN);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    public boolean sceneInTable(AllocatorDomainVO domain, AllocatorDomainScene scene) {
        if (domain.getScenes() == null) {
            return false;
        }
        return domain.getScenes().stream().anyMatch(s -> s.getScene() == scene);
    }

    private String readConstraintUuid(String vmUuid, AllocatorDomainScene scene) {
        if (scene == null) {
            return null;
        }
        switch (scene) {
            case NewCreate:
            case Start:
                return readConfigDomainUuid(vmUuid);
            case Migrate:
            case Ha:
            case HostMaintenance:
            case Drs:
                return readTagDomainUuid(vmUuid);
            default:
                return null;
        }
    }

    private String readConfigDomainUuid(String vmUuid) {
        String value = rcf.getResourceConfigValue(AllocatorDomainGlobalConfig.SCHEDULING_DOMAIN_UUID, vmUuid, String.class);
        if (StringUtils.isEmpty(value) || AllocatorDomainConstant.UNBOUND_DOMAIN_UUID.equals(value)) {
            return null;
        }
        return value;
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
}
