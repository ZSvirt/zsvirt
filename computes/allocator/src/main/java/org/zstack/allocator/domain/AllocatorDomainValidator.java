package org.zstack.allocator.domain;

import org.apache.commons.lang.StringUtils;
import org.zstack.allocator.domain.api.APICreateAllocatorDomainMsg;
import org.zstack.allocator.domain.api.APIUpdateAllocatorDomainMsg;
import org.zstack.allocator.domain.entity.AllocatorDomainRuleParam;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorDomainVO_;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.header.AllocatorDomainResourceType;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.err;

public class AllocatorDomainValidator {
    private static final String QUALIFIER_ROOT = "Root";
    private static final String QUALIFIER_DATA = "Data";

    public void validateCreate(APICreateAllocatorDomainMsg msg) {
        validateRules(msg.getZoneUuid(), msg.getRules());
    }

    public void validateUpdate(APIUpdateAllocatorDomainMsg msg) {
        if (msg.getRules() == null) {
            return;
        }
        AllocatorDomainVO domain = Q.New(AllocatorDomainVO.class)
                .eq(AllocatorDomainVO_.uuid, msg.getUuid())
                .find();
        validateRules(domain.getZoneUuid(), msg.getRules());
    }

    public void validateRules(String zoneUuid, List<AllocatorDomainRuleParam> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        Set<String> seenFull = new HashSet<>();
        Map<String, AllocatorDomainRuleEffect> hardBySelector = new HashMap<>();

        for (AllocatorDomainRuleParam rule : rules) {
            rejectReservedFields(rule);
            rewritePortGroup(rule);
            AllocatorDomainRuleEffect effect = parseEffect(rule);
            AllocatorDomainResourceType type = parseResourceType(rule);
            validateOne(zoneUuid, type, rule);

            String selector = selectorKey(rule);
            String full = selector + "\0" + effect.name();
            if (!seenFull.add(full)) {
                ErrorCode error = err(AllocatorDomainErrors.DUPLICATE_RULE,
                        "duplicate allocator domain rule for resource[type:%s, uuid:%s, qualifier:%s, effect:%s]",
                        rule.getResourceType(), rule.getResourceUuid(), normalizeQualifier(rule.getResourceQualifier()), effect.name())
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, rule.getResourceType())
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_UUID, rule.getResourceUuid())
                        .withOpaque(AllocatorDomainConstant.Opaque.EFFECT, effect.name());
                appendQualifierOpaque(error, rule.getResourceQualifier());
                intercept(error);
            }

            if (effect == AllocatorDomainRuleEffect.Include || effect == AllocatorDomainRuleEffect.Exclude) {
                AllocatorDomainRuleEffect previous = hardBySelector.put(selector, effect);
                if (previous != null && previous != effect) {
                    ErrorCode error = err(AllocatorDomainErrors.CONFLICTING_INCLUDE_EXCLUDE,
                            "allocator domain rule for resource[type:%s, uuid:%s] cannot have both Include and Exclude",
                            rule.getResourceType(), rule.getResourceUuid())
                            .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, rule.getResourceType())
                            .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_UUID, rule.getResourceUuid());
                    appendQualifierOpaque(error, rule.getResourceQualifier());
                    intercept(error);
                }
            }
        }
    }

    private void rejectReservedFields(AllocatorDomainRuleParam rule) {
        rejectIfPresent(rule.getResourcePolicy(), "resourcePolicy");
        rejectIfPresent(rule.getPolicyFrom(), "policyFrom");
        rejectIfPresent(rule.getPolicyUuid(), "policyUuid");
    }

    private void rejectIfPresent(String value, String fieldName) {
        if (!StringUtils.isEmpty(value)) {
            intercept(err(SysErrors.NOT_SUPPORTED,
                    "allocator domain rule field %s is reserved and must be empty", fieldName));
        }
    }

    private void rewritePortGroup(AllocatorDomainRuleParam rule) {
        if (AllocatorDomainConstant.PORT_GROUP_RESOURCE_TYPE.equals(rule.getResourceType())) {
            rule.setResourceType(AllocatorDomainResourceType.L3NetworkVO.toString());
        }
    }

    private AllocatorDomainRuleEffect parseEffect(AllocatorDomainRuleParam rule) {
        try {
            return AllocatorDomainRuleEffect.valueOf(rule.getEffect());
        } catch (IllegalArgumentException | NullPointerException e) {
            intercept(err(AllocatorDomainErrors.INVALID_RULE_EFFECT,
                    "effect[%s] is not a valid allocator domain rule effect", rule.getEffect())
                    .withOpaque(AllocatorDomainConstant.Opaque.EFFECT, rule.getEffect()));
            return null;
        }
    }

    private AllocatorDomainResourceType parseResourceType(AllocatorDomainRuleParam rule) {
        try {
            return AllocatorDomainResourceType.valueOf(rule.getResourceType());
        } catch (IllegalArgumentException | NullPointerException e) {
            ErrorCode error = err(AllocatorDomainErrors.INVALID_RULE,
                    "resourceType[%s] is not a supported allocator domain resource type", rule.getResourceType())
                    .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, rule.getResourceType());
            if (rule.getResourceUuid() != null) {
                error.withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_UUID, rule.getResourceUuid());
            }
            intercept(error);
            return null;
        }
    }

    private void validateOne(String zoneUuid, AllocatorDomainResourceType type, AllocatorDomainRuleParam rule) {
        switch (type) {
            case HostVO:
                validateHost(zoneUuid, rule);
                break;
            case ClusterVO:
                validateCluster(zoneUuid, rule);
                break;
            case PrimaryStorageVO:
                validatePrimaryStorage(zoneUuid, rule);
                break;
            case L3NetworkVO:
                validateL3(zoneUuid, rule);
                break;
            case BackupStorageVO:
                validateBackupStorage(zoneUuid, rule);
                break;
            default:
                intercept(err(AllocatorDomainErrors.INVALID_RULE,
                        "resourceType[%s] is not a supported allocator domain resource type", rule.getResourceType())
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, rule.getResourceType())
                        .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_UUID, rule.getResourceUuid()));
        }
    }

    private void validateHost(String zoneUuid, AllocatorDomainRuleParam rule) {
        rejectQualifierIfPresent(rule, AllocatorDomainErrors.HOST_QUALIFIER_NOT_ALLOWED,
                AllocatorDomainConstant.Opaque.HOST_UUID,
                "host[uuid:%s] allocator domain rule must not set resourceQualifier");
        HostVO vo = Q.New(HostVO.class).eq(HostVO_.uuid, rule.getResourceUuid()).find();
        if (vo == null) {
            intercept(notFound(AllocatorDomainErrors.HOST_NOT_FOUND,
                    AllocatorDomainConstant.Opaque.HOST_UUID, rule,
                    "host[uuid:%s] is not found or is not a HostVO"));
        }
        if (!zoneUuid.equals(vo.getZoneUuid())) {
            intercept(notInZone(AllocatorDomainErrors.HOST_NOT_IN_ZONE,
                    AllocatorDomainConstant.Opaque.HOST_UUID, rule.getResourceUuid(), zoneUuid,
                    "host[uuid:%s] is not in zone[uuid:%s]"));
        }
    }

    private void validateCluster(String zoneUuid, AllocatorDomainRuleParam rule) {
        rejectQualifierIfPresent(rule, AllocatorDomainErrors.CLUSTER_QUALIFIER_NOT_ALLOWED,
                AllocatorDomainConstant.Opaque.CLUSTER_UUID,
                "cluster[uuid:%s] allocator domain rule must not set resourceQualifier");
        ClusterVO vo = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, rule.getResourceUuid()).find();
        if (vo == null) {
            intercept(notFound(AllocatorDomainErrors.CLUSTER_NOT_FOUND,
                    AllocatorDomainConstant.Opaque.CLUSTER_UUID, rule,
                    "cluster[uuid:%s] is not found or is not a ClusterVO"));
        }
        if (!zoneUuid.equals(vo.getZoneUuid())) {
            intercept(notInZone(AllocatorDomainErrors.CLUSTER_NOT_IN_ZONE,
                    AllocatorDomainConstant.Opaque.CLUSTER_UUID, rule.getResourceUuid(), zoneUuid,
                    "cluster[uuid:%s] is not in zone[uuid:%s]"));
        }
    }

    private void validatePrimaryStorage(String zoneUuid, AllocatorDomainRuleParam rule) {
        String qualifier = rule.getResourceQualifier();
        if (!StringUtils.isEmpty(qualifier)
                && !QUALIFIER_ROOT.equals(qualifier)
                && !QUALIFIER_DATA.equals(qualifier)) {
            intercept(err(AllocatorDomainErrors.PRIMARY_STORAGE_INVALID_QUALIFIER,
                    "primary storage[uuid:%s] resourceQualifier[%s] must be empty, Root, or Data",
                    rule.getResourceUuid(), qualifier)
                    .withOpaque(AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, rule.getResourceUuid())
                    .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER, qualifier));
        }
        PrimaryStorageVO vo = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, rule.getResourceUuid()).find();
        if (vo == null) {
            intercept(notFound(AllocatorDomainErrors.PRIMARY_STORAGE_NOT_FOUND,
                    AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, rule,
                    "primary storage[uuid:%s] is not found or is not a PrimaryStorageVO"));
        }
        if (!zoneUuid.equals(vo.getZoneUuid())) {
            intercept(notInZone(AllocatorDomainErrors.PRIMARY_STORAGE_NOT_IN_ZONE,
                    AllocatorDomainConstant.Opaque.PRIMARY_STORAGE_UUID, rule.getResourceUuid(), zoneUuid,
                    "primary storage[uuid:%s] is not in zone[uuid:%s]"));
        }
    }

    private void validateL3(String zoneUuid, AllocatorDomainRuleParam rule) {
        rejectQualifierIfPresent(rule, AllocatorDomainErrors.L3_QUALIFIER_NOT_ALLOWED,
                AllocatorDomainConstant.Opaque.L3_UUID,
                "l3[uuid:%s] allocator domain rule must not set resourceQualifier");
        L3NetworkVO vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, rule.getResourceUuid()).find();
        if (vo == null) {
            intercept(notFound(AllocatorDomainErrors.L3_NOT_FOUND,
                    AllocatorDomainConstant.Opaque.L3_UUID, rule,
                    "l3[uuid:%s] is not found or is not an L3NetworkVO"));
        }
        if (!zoneUuid.equals(vo.getZoneUuid())) {
            intercept(notInZone(AllocatorDomainErrors.L3_NOT_IN_ZONE,
                    AllocatorDomainConstant.Opaque.L3_UUID, rule.getResourceUuid(), zoneUuid,
                    "l3[uuid:%s] is not in zone[uuid:%s]"));
        }
    }

    private void validateBackupStorage(String zoneUuid, AllocatorDomainRuleParam rule) {
        rejectQualifierIfPresent(rule, AllocatorDomainErrors.BACKUP_STORAGE_QUALIFIER_NOT_ALLOWED,
                AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID,
                "backup storage[uuid:%s] allocator domain rule must not set resourceQualifier");
        BackupStorageVO vo = Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, rule.getResourceUuid()).find();
        if (vo == null) {
            intercept(notFound(AllocatorDomainErrors.BACKUP_STORAGE_NOT_FOUND,
                    AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID, rule,
                    "backup storage[uuid:%s] is not found or is not a BackupStorageVO"));
        }
        boolean inZone = Q.New(BackupStorageZoneRefVO.class)
                .eq(BackupStorageZoneRefVO_.backupStorageUuid, rule.getResourceUuid())
                .eq(BackupStorageZoneRefVO_.zoneUuid, zoneUuid)
                .isExists();
        if (!inZone) {
            intercept(notInZone(AllocatorDomainErrors.BACKUP_STORAGE_NOT_IN_ZONE,
                    AllocatorDomainConstant.Opaque.BACKUP_STORAGE_UUID, rule.getResourceUuid(), zoneUuid,
                    "backup storage[uuid:%s] is not attached to zone[uuid:%s]"));
        }
    }

    private void rejectQualifierIfPresent(AllocatorDomainRuleParam rule, AllocatorDomainErrors code,
                                          String uuidOpaqueKey, String details) {
        if (!StringUtils.isEmpty(rule.getResourceQualifier())) {
            intercept(err(code, details, rule.getResourceUuid())
                    .withOpaque(uuidOpaqueKey, rule.getResourceUuid())
                    .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER, rule.getResourceQualifier()));
        }
    }

    private ErrorCode notFound(AllocatorDomainErrors code, String uuidOpaqueKey,
                               AllocatorDomainRuleParam rule, String details) {
        return err(code, details, rule.getResourceUuid())
                .withOpaque(uuidOpaqueKey, rule.getResourceUuid())
                .withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_TYPE, rule.getResourceType());
    }

    private ErrorCode notInZone(AllocatorDomainErrors code, String uuidOpaqueKey,
                                String resourceUuid, String zoneUuid, String details) {
        return err(code, details, resourceUuid, zoneUuid)
                .withOpaque(uuidOpaqueKey, resourceUuid)
                .withOpaque(AllocatorDomainConstant.Opaque.ZONE_UUID, zoneUuid);
    }

    private void appendQualifierOpaque(ErrorCode error, String qualifier) {
        if (!StringUtils.isEmpty(qualifier)) {
            error.withOpaque(AllocatorDomainConstant.Opaque.RESOURCE_QUALIFIER, qualifier);
        }
    }

    private String selectorKey(AllocatorDomainRuleParam rule) {
        return rule.getResourceType() + "\0" + rule.getResourceUuid() + "\0" + normalizeQualifier(rule.getResourceQualifier());
    }

    private String normalizeQualifier(String qualifier) {
        return StringUtils.isEmpty(qualifier) ? "" : qualifier;
    }

    private void intercept(ErrorCode error) {
        throw new ApiMessageInterceptionException(error);
    }
}
