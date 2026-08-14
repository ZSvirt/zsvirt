package org.zstack.compute.vm.metadata;

import org.apache.commons.lang.StringUtils;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO_;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceSequenceNumberVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class MetadataRegistrationPersistHelper {
    private static final CLogger logger = Utils.getLogger(MetadataRegistrationPersistHelper.class);

    private MetadataRegistrationPersistHelper() {
    }

    /**
     * Holds pre-decoded system tags and resource configs for a single resource (VM or Volume).
     * Populated during {@link #validateMetadataContent} so that the persist phase can use
     * the already-parsed objects directly without a second decode pass.
     */
    public static class ParsedTagsAndConfigs {
        public final List<SystemTagVO> systemTags;
        public final List<ResourceConfigVO> resourceConfigs;

        public ParsedTagsAndConfigs(List<SystemTagVO> systemTags, List<ResourceConfigVO> resourceConfigs) {
            this.systemTags = systemTags;
            this.resourceConfigs = resourceConfigs;
        }
    }

    public static class PersistContext {
        public final VmInstanceMetadataDTO dto;
        public final String vmUuid;
        public final String primaryStorageUuid;
        public final String psType;
        public final Map<String, String> metadataToCurrentPathMap;
        public final String zoneUuid;
        public final String clusterUuid;
        public final String hostUuid;
        public final String accountUuid;
        public final String vmName;

        /**
         * Pre-decoded tags and configs keyed by resourceUuid.
         * Populated by {@link #validateMetadataContent}, consumed by persist methods.
         */
        public final Map<String, ParsedTagsAndConfigs> parsedTagsAndConfigsMap;

        public PersistContext(VmInstanceMetadataDTO dto, String vmUuid,
                              String primaryStorageUuid, String psType,
                              Map<String, String> metadataToCurrentPathMap,
                              String zoneUuid, String clusterUuid,
                              String hostUuid, String accountUuid,
                              String vmName,
                              Map<String, ParsedTagsAndConfigs> parsedTagsAndConfigsMap) {
            this.dto = dto;
            this.vmUuid = vmUuid;
            this.primaryStorageUuid = primaryStorageUuid;
            this.psType = psType;
            this.metadataToCurrentPathMap = metadataToCurrentPathMap;
            this.zoneUuid = zoneUuid;
            this.clusterUuid = clusterUuid;
            this.hostUuid = hostUuid;
            this.accountUuid = accountUuid;
            this.vmName = vmName;
            this.parsedTagsAndConfigsMap = parsedTagsAndConfigsMap != null
                    ? parsedTagsAndConfigsMap : Collections.emptyMap();
        }
    }

    /**
     * Result of {@link #validateMetadataContent}. On success, {@code error} is null
     * and {@code parsedTagsAndConfigsMap} contains pre-decoded tags/configs for every
     * resource. On failure, {@code error} is non-null.
     */
    public static class ValidateResult {
        public final ErrorCode error;
        public final Map<String, ParsedTagsAndConfigs> parsedTagsAndConfigsMap;

        private ValidateResult(ErrorCode error, Map<String, ParsedTagsAndConfigs> map) {
            this.error = error;
            this.parsedTagsAndConfigsMap = map;
        }

        public static ValidateResult success(Map<String, ParsedTagsAndConfigs> map) {
            return new ValidateResult(null, map);
        }

        public static ValidateResult fail(ErrorCode error) {
            return new ValidateResult(error, null);
        }
    }

    public static ValidateResult validateMetadataContent(VmInstanceMetadataDTO dto) {
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        if (dto == null || dto.getVm() == null || StringUtils.isBlank(dto.getVm().getResourceUuid())) {
            return ValidateResult.fail(Platform.operr("metadata payload is missing or invalid: dto, vm block, or vm resourceUuid is null/empty"));
        }
        if (StringUtils.isBlank(dto.getVm().getVo())) {
            return ValidateResult.fail(Platform.operr("metadata payload is invalid: vm.vo is null or empty"));
        }
        if (dto.getVmCategory() == null) {
            return ValidateResult.fail(Platform.operr("metadata vmCategory is missing"));
        }

        logger.info(String.format("[MetadataRegistration] validateMetadataContent START: vmUuid=%s, vmCategory=%s, schemaVersion=%s",
                dto.getVm() != null ? dto.getVm().getResourceUuid() : "null",
                dto.getVmCategory(), dto.getSchemaVersion()));

        String currentDbVersion = dbf.getDbVersion();
        logger.debug(String.format("[MetadataRegistration] validateMetadataContent: currentDbVersion=%s, metadataSchemaVersion=%s",
                currentDbVersion, dto.getSchemaVersion()));
        if (!StringUtils.equals(dto.getSchemaVersion(), currentDbVersion)) {
            logger.warn(String.format("[MetadataRegistration] metadata schemaVersion[%s] does not match current database version[%s], " +
                    "registration may encounter compatibility issues", dto.getSchemaVersion(), currentDbVersion));
        }

        if (dto.getVmCategory() == VmMetadataCategory.VM_TEMPLATE_CACHE) {
            logger.warn("[MetadataRegistration] validateMetadataContent REJECTED: vmCategory=VM_TEMPLATE_CACHE");
            return ValidateResult.fail(Platform.operr("cannot register VM with vmCategory=VM_TEMPLATE_CACHE"));
        }

        if (dto.getVolumes() == null || dto.getVolumes().isEmpty()) {
            return ValidateResult.fail(Platform.operr("metadata volumes list is null or empty, " +
                    "VM must have at least a root volume"));
        }

        {
            Set<String> psUuids = new LinkedHashSet<>();
            int rootVolumeCount = 0;
            for (ResourceMetadata vol : dto.getVolumes()) {
                VolumeVO v;
                try {
                    v = JSONObjectUtil.toObject(vol.getVo(), VolumeVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse volume metadata JSON: %s", e.getMessage()));
                }
                if (StringUtils.isNotEmpty(v.getPrimaryStorageUuid())) {
                    psUuids.add(v.getPrimaryStorageUuid());
                }
                if (v.getType() == VolumeType.Root) {
                    rootVolumeCount++;
                }
            }
            logger.debug(String.format("[MetadataRegistration] validateMetadataContent: volumeCount=%d, rootVolumeCount=%d, distinctPsUuids=%s",
                    dto.getVolumes().size(), rootVolumeCount, psUuids));
            if (psUuids.size() > 1) {
                return ValidateResult.fail(Platform.operr("all volumes must come from the same primary storage, but found multiple primaryStorageUuids: %s", psUuids));
            }
            if (rootVolumeCount != 1) {
                return ValidateResult.fail(Platform.operr("metadata must contain exactly 1 root volume, but found %d", rootVolumeCount));
            }
        }

        // Collect UUIDs per VO type
        String vmUuid = dto.getVm().getResourceUuid();

        Set<String> volumeUuids = new LinkedHashSet<>();
        if (dto.getVolumes() != null) {
            for (ResourceMetadata vol : dto.getVolumes()) {
                VolumeVO v;
                try {
                    v = JSONObjectUtil.toObject(vol.getVo(), VolumeVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse volume metadata JSON: %s", e.getMessage()));
                }
                volumeUuids.add(v.getUuid());
            }
        }

        Set<String> snapshotUuids = new LinkedHashSet<>();
        Set<String> snapshotTreeUuids = new LinkedHashSet<>();
        if (dto.getSnapshots() != null) {
            List<String> orphanSnapshotUuids = new ArrayList<>();
            for (String sJson : dto.getSnapshots()) {
                VolumeSnapshotVO s;
                try {
                    s = JSONObjectUtil.toObject(sJson, VolumeSnapshotVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse snapshot metadata JSON: %s", e.getMessage()));
                }
                snapshotUuids.add(s.getUuid());
                if (s.getTreeUuid() != null) {
                    snapshotTreeUuids.add(s.getTreeUuid());
                }
                if (s.getVolumeUuid() != null && !volumeUuids.contains(s.getVolumeUuid())) {
                    orphanSnapshotUuids.add(s.getUuid());
                }
            }
            if (!orphanSnapshotUuids.isEmpty()) {
                return ValidateResult.fail(Platform.operr(
                        "snapshot(s) %s reference volumeUuid not present in metadata volume set %s",
                        orphanSnapshotUuids, volumeUuids));
            }
        }

        Set<String> snapshotGroupUuids = new LinkedHashSet<>();
        if (dto.getSnapshotGroups() != null) {
            for (String groupJson : dto.getSnapshotGroups()) {
                VolumeSnapshotGroupVO group;
                try {
                    group = JSONObjectUtil.toObject(groupJson, VolumeSnapshotGroupVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse snapshot group metadata JSON: %s", e.getMessage()));
                }
                snapshotGroupUuids.add(group.getUuid());
            }
        }

        // Pre-validate JSON for snapshotGroupRefs, snapshotReferenceTree, snapshotReference
        // to catch corruption early rather than failing mid-persist
        if (dto.getSnapshotGroupRefs() != null) {
            for (String refJson : dto.getSnapshotGroupRefs()) {
                try {
                    JSONObjectUtil.toObject(refJson, VolumeSnapshotGroupRefVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse snapshot group ref metadata JSON: %s", e.getMessage()));
                }
            }
        }
        if (dto.getSnapshotTrees() != null) {
            for (String treeJson : dto.getSnapshotTrees()) {
                VolumeSnapshotTreeVO treeMeta;
                try {
                    treeMeta = JSONObjectUtil.toObject(treeJson, VolumeSnapshotTreeVO.class);
                } catch (Exception e) {
                    return ValidateResult.fail(Platform.operr("failed to parse snapshot tree metadata JSON: %s", e.getMessage()));
                }
                if (treeMeta == null || StringUtils.isBlank(treeMeta.getUuid())) {
                    return ValidateResult.fail(Platform.operr(
                            "invalid snapshotTrees metadata: tree uuid is null or empty, json=%s", treeJson));
                }
            }
        }
        if (dto.getVolumes() != null) {
            for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
                if (volMeta.getSnapshotReferenceTree() != null) {
                    try {
                        JSONObjectUtil.toObject(volMeta.getSnapshotReferenceTree(), VolumeSnapshotReferenceTreeVO.class);
                    } catch (Exception e) {
                        return ValidateResult.fail(Platform.operr("failed to parse snapshot reference tree metadata JSON: %s", e.getMessage()));
                    }
                }
                if (volMeta.getSnapshotReference() != null) {
                    try {
                        JSONObjectUtil.toObject(volMeta.getSnapshotReference(), VolumeSnapshotReferenceVO.class);
                    } catch (Exception e) {
                        return ValidateResult.fail(Platform.operr("failed to parse snapshot reference metadata JSON: %s", e.getMessage()));
                    }
                }
            }
        }

        logger.debug(String.format("[MetadataRegistration] validateMetadataContent: vmUuid=%s, volumeUuids=%s, " +
                        "snapshotUuids=%s, snapshotTreeUuids=%s, snapshotGroupUuids=%s",
                vmUuid, volumeUuids, snapshotUuids, snapshotTreeUuids, snapshotGroupUuids));

        logger.debug(String.format("[MetadataRegistration] checking VM UUID conflict: vmUuid=%s", vmUuid));
        VmInstanceVO existingVm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (existingVm != null) {
            logger.warn(String.format("[MetadataRegistration] validateMetadataContent REJECTED: VmInstanceVO[uuid:%s] already exists in state[%s]",
                    vmUuid, existingVm.getState()));
            return ValidateResult.fail(Platform.operr("UUID conflict detected for VmInstanceVO: %s", vmUuid));
        }

        // Batch check each VO type with its own UUID set
        // NOTE: For types that have EO (soft-delete) tables (VolumeSnapshotTree, VolumeSnapshot, Volume),
        // we must query the EO table instead of the VO view. The VO view filters out soft-deleted records
        // (deleted IS NOT NULL), but dbf.persist() inserts into the EO table directly, so a soft-deleted
        // EO record with the same UUID will cause a PRIMARY KEY conflict.
        Set<String> conflicts = new LinkedHashSet<>();
        Map<String, String> softDeletedConflicts = new LinkedHashMap<>();
        List<String> found;

        if (!volumeUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] checking VolumeEO UUID conflicts (including soft-deleted): %s", volumeUuids));
            found = SQL.New("SELECT vo.uuid FROM VolumeEO vo WHERE vo.uuid IN (:uuids)", String.class)
                    .param("uuids", volumeUuids)
                    .list();
            if (found != null && !found.isEmpty()) {
                // Distinguish active vs soft-deleted
                List<String> activeVols = Q.New(VolumeVO.class).select(VolumeVO_.uuid).in(VolumeVO_.uuid, found).listValues();
                Set<String> activeSet = activeVols != null ? new LinkedHashSet<>(activeVols) : Collections.emptySet();
                for (String uuid : found) {
                    if (activeSet.contains(uuid)) {
                        logger.warn(String.format("[MetadataRegistration] VolumeVO UUID conflict (active): %s", uuid));
                        conflicts.add(uuid);
                    } else {
                        logger.warn(String.format("[MetadataRegistration] VolumeEO UUID conflict (soft-deleted residual): %s", uuid));
                        softDeletedConflicts.put(uuid, "VolumeEO");
                    }
                }
            }
        }

        if (!snapshotUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] checking VolumeSnapshotEO UUID conflicts (including soft-deleted): %s", snapshotUuids));
            found = Q.New(VolumeSnapshotEO.class).select(VolumeSnapshotEO_.uuid).in(VolumeSnapshotEO_.uuid, snapshotUuids).listValues();
            if (found != null && !found.isEmpty()) {
                List<String> activeSnaps = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid).in(VolumeSnapshotVO_.uuid, found).listValues();
                Set<String> activeSet = activeSnaps != null ? new LinkedHashSet<>(activeSnaps) : Collections.emptySet();
                for (String uuid : found) {
                    if (activeSet.contains(uuid)) {
                        logger.warn(String.format("[MetadataRegistration] VolumeSnapshotVO UUID conflict (active): %s", uuid));
                        conflicts.add(uuid);
                    } else {
                        logger.warn(String.format("[MetadataRegistration] VolumeSnapshotEO UUID conflict (soft-deleted residual): %s", uuid));
                        softDeletedConflicts.put(uuid, "VolumeSnapshotEO");
                    }
                }
            }
        }

        if (!snapshotTreeUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] checking VolumeSnapshotTreeEO UUID conflicts (including soft-deleted): %s", snapshotTreeUuids));
            found = Q.New(VolumeSnapshotTreeEO.class).select(VolumeSnapshotTreeEO_.uuid).in(VolumeSnapshotTreeEO_.uuid, snapshotTreeUuids).listValues();
            if (found != null && !found.isEmpty()) {
                List<String> activeTrees = Q.New(VolumeSnapshotTreeVO.class).select(VolumeSnapshotTreeVO_.uuid).in(VolumeSnapshotTreeVO_.uuid, found).listValues();
                Set<String> activeSet = activeTrees != null ? new LinkedHashSet<>(activeTrees) : Collections.emptySet();
                for (String uuid : found) {
                    if (activeSet.contains(uuid)) {
                        logger.warn(String.format("[MetadataRegistration] VolumeSnapshotTreeVO UUID conflict (active): %s", uuid));
                        conflicts.add(uuid);
                    } else {
                        logger.warn(String.format("[MetadataRegistration] VolumeSnapshotTreeEO UUID conflict (soft-deleted residual): %s", uuid));
                        softDeletedConflicts.put(uuid, "VolumeSnapshotTreeEO");
                    }
                }
            }
        }

        if (!snapshotGroupUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] checking VolumeSnapshotGroupVO UUID conflicts: %s", snapshotGroupUuids));
            found = Q.New(VolumeSnapshotGroupVO.class).select(VolumeSnapshotGroupVO_.uuid).in(VolumeSnapshotGroupVO_.uuid, snapshotGroupUuids).listValues();
            if (found != null && !found.isEmpty()) {
                logger.warn(String.format("[MetadataRegistration] VolumeSnapshotGroupVO UUID conflicts found: %s", found));
                conflicts.addAll(found);
            }
        }

        if (!conflicts.isEmpty()) {
            logger.warn(String.format("[MetadataRegistration] validateMetadataContent REJECTED: active UUID conflicts for %s", conflicts));
            return ValidateResult.fail(Platform.operr("UUID conflict detected for resources: %s", conflicts));
        }

        if (!softDeletedConflicts.isEmpty()) {
            List<String> details = new ArrayList<>();
            for (Map.Entry<String, String> e : softDeletedConflicts.entrySet()) {
                details.add(String.format("%s(%s)", e.getKey(), e.getValue()));
            }
            String detailStr = StringUtils.join(details, ", ");
            logger.warn(String.format("[MetadataRegistration] validateMetadataContent REJECTED: soft-deleted EO residuals block registration: [%s]. " +
                    "These are leftover records from previously deleted resources that were not fully cleaned up. " +
                    "Please clean up the EO table residuals before retrying.", detailStr));
            return ValidateResult.fail(Platform.operr("registration blocked by soft-deleted EO residuals (from previously deleted resources): [%s]. " +
                    "These records exist in the database EO tables but not in the active VO views. " +
                    "Please contact support to clean up these residual records.", detailStr));
        }

        // Pre-decode and validate all system tags and resource configs.
        // If any entry is malformed, we fail fast here before any VO is persisted.
        Map<String, ParsedTagsAndConfigs> parsedMap = new HashMap<>();
        try {
            // VM tags/configs
            parsedMap.put(vmUuid, new ParsedTagsAndConfigs(
                    MetadataRegistrationUtils.decodeAndValidateSystemTags(dto.getVm().getSystemTags(), vmUuid),
                    MetadataRegistrationUtils.decodeAndValidateResourceConfigs(dto.getVm().getResourceConfigs(), vmUuid)));

            // Volume tags/configs
            if (dto.getVolumes() != null) {
                for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
                    VolumeVO vol = JSONObjectUtil.toObject(volMeta.getVo(), VolumeVO.class);
                    String volUuid = vol.getUuid();
                    parsedMap.put(volUuid, new ParsedTagsAndConfigs(
                            MetadataRegistrationUtils.decodeAndValidateSystemTags(volMeta.getSystemTags(), volUuid),
                            MetadataRegistrationUtils.decodeAndValidateResourceConfigs(volMeta.getResourceConfigs(), volUuid)));
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("[MetadataRegistration] validateMetadataContent REJECTED: failed to decode tags/configs: %s",
                    e.getMessage()), e);
            return ValidateResult.fail(Platform.operr(
                    "metadata tags/configs decode failed: %s", e.getMessage()));
        }

        logger.info(String.format("[MetadataRegistration] validateMetadataContent PASSED: vmUuid=%s", vmUuid));
        return ValidateResult.success(parsedMap);
    }

    public static void persistVmAndResources(PersistContext ctx) {
        logger.info(String.format("[MetadataRegistration] persistVmAndResources START: vmUuid=%s, psUuid=%s, psType=%s, " +
                        "volumes=%d, snapshots=%d, snapshotGroups=%d",
                ctx.vmUuid, ctx.primaryStorageUuid, ctx.psType,
                ctx.dto.getVolumes() != null ? ctx.dto.getVolumes().size() : 0,
                ctx.dto.getSnapshots() != null ? ctx.dto.getSnapshots().size() : 0,
                ctx.dto.getSnapshotGroups() != null ? ctx.dto.getSnapshotGroups().size() : 0));

        Set<String> createdVoUuids = new LinkedHashSet<>();

        persistVmInstance(ctx);
        logger.debug(String.format("[MetadataRegistration] persistVmInstance done for vm=%s", ctx.vmUuid));

        persistVolumes(ctx, createdVoUuids);
        logger.debug(String.format("[MetadataRegistration] persistVolumes done for vm=%s, createdVOs=%d", ctx.vmUuid, createdVoUuids.size()));

        persistSnapshots(ctx, createdVoUuids);
        logger.debug(String.format("[MetadataRegistration] persistSnapshots done for vm=%s, totalCreatedVOs=%d", ctx.vmUuid, createdVoUuids.size()));

        finalizeRegistration(ctx);
        logger.info(String.format("[MetadataRegistration] persistVmAndResources COMPLETE: vmUuid=%s", ctx.vmUuid));
    }

    private static void finalizeRegistration(PersistContext ctx) {
        logger.debug(String.format("[MetadataRegistration] finalizeRegistration: querying root volume for vm[uuid:%s]", ctx.vmUuid));

        String rootVolumeUuid = Q.New(VolumeVO.class).select(VolumeVO_.uuid).eq(VolumeVO_.vmInstanceUuid, ctx.vmUuid)
                .eq(VolumeVO_.type, VolumeType.Root).findValue();
        if (rootVolumeUuid == null) {
            throw new CloudRuntimeException(String.format(
                    "cannot finalize registration for vm[uuid:%s]: no root volume found. " +
                            "The metadata may be incomplete or volume persist failed.", ctx.vmUuid));
        }

        logger.info(String.format("[MetadataRegistration] finalizeRegistration: vm[uuid:%s] -> state=Stopped, rootVolumeUuid=%s",
                ctx.vmUuid, rootVolumeUuid));

        SQL.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, ctx.vmUuid)
                .set(VmInstanceVO_.state, VmInstanceState.Stopped)
                .set(VmInstanceVO_.rootVolumeUuid, rootVolumeUuid)
                .set(VmInstanceVO_.lastOpDate, new Timestamp(System.currentTimeMillis()))
                .update();

        logger.debug(String.format("[MetadataRegistration] finalizeRegistration: removing VM_METADATA_REGISTERING_MN_UUID tag for vm[uuid:%s]",
                ctx.vmUuid));
        VmSystemTags.VM_METADATA_REGISTERING_MN_UUID.delete(ctx.vmUuid);

        logger.info(String.format("[MetadataRegistration] finalizeRegistration COMPLETE: vm[uuid:%s] -> Stopped, rootVolumeUuid=%s",
                ctx.vmUuid, rootVolumeUuid));
    }

    private static void persistVmInstance(PersistContext ctx) {
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        logger.debug(String.format("[MetadataRegistration] persistVmInstance: checking existing VM[uuid:%s]", ctx.vmUuid));

        VmInstanceVO vmVO = JSONObjectUtil.toObject(ctx.dto.getVm().getVo(), VmInstanceVO.class);
        vmVO.setUuid(ctx.vmUuid);
        vmVO.setZoneUuid(ctx.zoneUuid);
        vmVO.setClusterUuid(ctx.clusterUuid);
        vmVO.setState(VmInstanceState.Registering);
        vmVO.setAccountUuid(ctx.accountUuid);
        vmVO.setRootVolumeUuid(null);
        vmVO.setHostUuid(null);
        vmVO.setLastHostUuid(ctx.hostUuid);
        vmVO.setInstanceOfferingUuid(null);
        vmVO.setDefaultL3NetworkUuid(null);
        vmVO.setImageUuid(null);
        vmVO.setCreateDate(now);
        vmVO.setLastOpDate(now);
        if (ctx.vmName != null && !ctx.vmName.isEmpty()) {
            vmVO.setName(ctx.vmName);
        }

        long originalInternalId = vmVO.getInternalId();
        long newInternalId = dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class);
        vmVO.setInternalId(newInternalId);
        logger.info(String.format("[MetadataRegistration] persistVmInstance: reassigned internalId for vm[uuid:%s]: %d -> %d",
                ctx.vmUuid, originalInternalId, newInternalId));

        logger.info(String.format("[MetadataRegistration] persisting VmInstanceVO[uuid:%s, name:%s, zoneUuid:%s, clusterUuid:%s, state:Registering]",
                vmVO.getUuid(), vmVO.getName(), vmVO.getZoneUuid(), vmVO.getClusterUuid()));
        try {
            dbf.persist(vmVO);
        } catch (javax.persistence.EntityExistsException e) {
            throw new CloudRuntimeException(String.format(
                    "vm[uuid:%s] already exists (concurrent registration race): %s",
                    vmVO.getUuid(), e.getMessage()), e);
        }
        logger.debug(String.format("[MetadataRegistration] VmInstanceVO[uuid:%s] persisted successfully", vmVO.getUuid()));

        logger.debug(String.format("[MetadataRegistration] creating VM_METADATA_REGISTERING_MN_UUID tag for vm[uuid:%s]", ctx.vmUuid));
        SystemTagCreator creator = VmSystemTags.VM_METADATA_REGISTERING_MN_UUID.newSystemTagCreator(ctx.vmUuid);
        creator.setTagByTokens(map(
                e(VmSystemTags.VM_METADATA_REGISTERING_MN_UUID_TOKEN, Platform.getManagementServerId())
        ));
        creator.ignoreIfExisting = true;
        creator.inherent = false;
        creator.recreate = true;
        creator.unique = true;
        creator.create();

        logger.debug(String.format("[MetadataRegistration] restoring system tags and resource configs for vm[uuid:%s]", ctx.vmUuid));
        ParsedTagsAndConfigs vmParsed = ctx.parsedTagsAndConfigsMap.get(ctx.vmUuid);
        if (vmParsed != null) {
            MetadataRegistrationUtils.persistSystemTags(dbf, vmParsed.systemTags, ctx.vmUuid, now);
            MetadataRegistrationUtils.persistResourceConfigs(dbf, vmParsed.resourceConfigs, ctx.vmUuid, now);
        }
    }

    private static void persistVolumes(PersistContext ctx, Set<String> createdVoUuids) {
        if (ctx.dto.getVolumes() == null) {
            return;
        }

        for (VolumeResourceMetadata volMeta : ctx.dto.getVolumes()) {
            VolumeVO vol = JSONObjectUtil.toObject(volMeta.getVo(), VolumeVO.class);
            persistVolume(ctx, volMeta, vol, createdVoUuids);
        }
    }

    private static void persistSnapshots(PersistContext ctx, Set<String> createdVoUuids) {
        PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (ctx.dto.getSnapshots() != null && !ctx.dto.getSnapshots().isEmpty()) {
            logger.info(String.format("[MetadataRegistration] persistSnapshots: parsing %d snapshot entries for vm=%s",
                    ctx.dto.getSnapshots().size(), ctx.vmUuid));

            // Build a map from treeUuid -> VolumeSnapshotTreeVO from metadata for current
            Map<String, VolumeSnapshotTreeVO> treeMetaMap = new HashMap<>();
            if (ctx.dto.getSnapshotTrees() != null) {
                for (String treeJson : ctx.dto.getSnapshotTrees()) {
                    VolumeSnapshotTreeVO treeMeta = JSONObjectUtil.toObject(treeJson, VolumeSnapshotTreeVO.class);
                    treeMetaMap.put(treeMeta.getUuid(), treeMeta);
                }
            }

            List<VolumeSnapshotVO> allSnaps = new ArrayList<>();
            for (String snapJson : ctx.dto.getSnapshots()) {
                allSnaps.add(JSONObjectUtil.toObject(snapJson, VolumeSnapshotVO.class));
            }

            Map<String, List<VolumeSnapshotVO>> byTree = new LinkedHashMap<>();
            for (VolumeSnapshotVO s : allSnaps) {
                String key = s.getTreeUuid() != null ? s.getTreeUuid() : "";
                byTree.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }

            logger.debug(String.format("[MetadataRegistration] persistSnapshots: %d trees, snapshot UUIDs=%s",
                    byTree.size(), allSnaps.stream().map(VolumeSnapshotVO::getUuid).collect(Collectors.toList())));

            Set<String> treeUuidsCreated = new HashSet<>();

            for (Map.Entry<String, List<VolumeSnapshotVO>> entry : byTree.entrySet()) {
                String treeUuid = entry.getKey();
                List<VolumeSnapshotVO> snapsInTree = entry.getValue();

                if (!treeUuid.isEmpty() && !treeUuidsCreated.contains(treeUuid)) {
                    boolean existsInVO = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, treeUuid).isExists();

                    if (!existsInVO) {
                        VolumeSnapshotTreeVO treeVO = new VolumeSnapshotTreeVO();
                        treeVO.setStatus(VolumeSnapshotTreeStatus.Completed);
                        treeVO.setUuid(treeUuid);
                        // prefer root snapshot (parentUuid == null) for volumeUuid; fallback to first element
                        String treeVolumeUuid = snapsInTree.get(0).getVolumeUuid();
                        for (VolumeSnapshotVO s : snapsInTree) {
                            if (s.getParentUuid() == null || s.getParentUuid().isEmpty()) {
                                treeVolumeUuid = s.getVolumeUuid();
                                break;
                            }
                        }
                        treeVO.setVolumeUuid(treeVolumeUuid);
                        // Restore current from metadata if available
                        VolumeSnapshotTreeVO treeMeta = treeMetaMap.get(treeUuid);
                        if (treeMeta != null) {
                            treeVO.setCurrent(treeMeta.isCurrent());
                        }
                        treeVO.setCreateDate(now);
                        treeVO.setLastOpDate(now);

                        logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotTreeVO[uuid:%s, volumeUuid:%s]",
                                treeVO.getUuid(), treeVO.getVolumeUuid()));
                        dbf.persist(treeVO);
                        createdVoUuids.add(treeVO.getUuid());
                    } else {
                        logger.debug(String.format("[MetadataRegistration] VolumeSnapshotTreeVO[uuid:%s] already exists, skipping", treeUuid));
                    }
                    treeUuidsCreated.add(treeUuid);
                }

                List<VolumeSnapshotVO> ordered = bfsOrderSnapshots(snapsInTree);
                for (VolumeSnapshotVO snap : ordered) {
                    snap.setPrimaryStorageUuid(ctx.primaryStorageUuid);
                    String newSnapPath = ctx.metadataToCurrentPathMap.get(snap.getPrimaryStorageInstallPath());
                    if (newSnapPath != null) {
                        snap.setPrimaryStorageInstallPath(newSnapPath);
                    }
                    snap.setLastOpDate(now);
                    if (snap.getStatus() == null) {
                        snap.setStatus(VolumeSnapshotStatus.Ready);
                    }

                    logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotVO[uuid:%s, volumeUuid:%s, treeUuid:%s, " +
                                    "parentUuid:%s, installPath:%s]",
                            snap.getUuid(), snap.getVolumeUuid(), snap.getTreeUuid(),
                            snap.getParentUuid(), snap.getPrimaryStorageInstallPath()));
                    dbf.persist(snap);
                    logger.debug(String.format("[MetadataRegistration] VolumeSnapshotVO[uuid:%s] persisted successfully", snap.getUuid()));
                    createdVoUuids.add(snap.getUuid());

                    logger.debug(String.format("[MetadataRegistration] calling afterSnapshotPersist extensions for snapshot[uuid:%s], psType=%s",
                            snap.getUuid(), ctx.psType));
                    for (VmMetadataResourcePersistExtensionPoint ext :
                            pluginRgty.getExtensionList(VmMetadataResourcePersistExtensionPoint.class)) {
                        if (ext.getPrimaryStorageType().equals(ctx.psType)) {
                            ext.afterSnapshotPersist(ctx.primaryStorageUuid, snap.getUuid(),
                                    VolumeSnapshotVO.class.getSimpleName(), ctx.hostUuid, snap.getSize(), now);
                            break;
                        }
                    }
                }
            }
            logger.debug(String.format("[MetadataRegistration] persistSnapshots: all %d snapshots persisted for vm=%s",
                    allSnaps.size(), ctx.vmUuid));
        }

        if (ctx.dto.getSnapshotGroups() != null) {
            logger.info(String.format("[MetadataRegistration] persisting %d VolumeSnapshotGroupVOs for vm=%s",
                    ctx.dto.getSnapshotGroups().size(), ctx.vmUuid));
            for (String groupJson : ctx.dto.getSnapshotGroups()) {
                VolumeSnapshotGroupVO group = JSONObjectUtil.toObject(groupJson, VolumeSnapshotGroupVO.class);
                group.setLastOpDate(now);

                logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotGroupVO[uuid:%s, vmInstanceUuid:%s, name:%s]",
                        group.getUuid(), group.getVmInstanceUuid(), group.getName()));
                dbf.persist(group);
                logger.debug(String.format("[MetadataRegistration] VolumeSnapshotGroupVO[uuid:%s] persisted successfully", group.getUuid()));
                createdVoUuids.add(group.getUuid());
            }
        }

        if (ctx.dto.getSnapshotGroupRefs() != null) {
            logger.info(String.format("[MetadataRegistration] persisting %d VolumeSnapshotGroupRefVOs for vm=%s",
                    ctx.dto.getSnapshotGroupRefs().size(), ctx.vmUuid));
            for (String refJson : ctx.dto.getSnapshotGroupRefs()) {
                VolumeSnapshotGroupRefVO ref = JSONObjectUtil.toObject(refJson, VolumeSnapshotGroupRefVO.class);
                String newRefPath = ctx.metadataToCurrentPathMap.get(ref.getVolumeSnapshotInstallPath());
                if (newRefPath != null) {
                    ref.setVolumeSnapshotInstallPath(newRefPath);
                }
                ref.setLastOpDate(now);

                logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotGroupRefVO[snapGroupUuid:%s, snapUuid:%s, volUuid:%s]",
                        ref.getVolumeSnapshotGroupUuid(), ref.getVolumeSnapshotUuid(), ref.getVolumeUuid()));
                dbf.persist(ref);
                logger.debug(String.format("[MetadataRegistration] VolumeSnapshotGroupRefVO[snapGroupUuid:%s, snapUuid:%s] persisted successfully",
                        ref.getVolumeSnapshotGroupUuid(), ref.getVolumeSnapshotUuid()));
            }
        }

        if (ctx.dto.getVolumes() != null) {
            for (VolumeResourceMetadata volMeta : ctx.dto.getVolumes()) {
                if (volMeta.getSnapshotReferenceTree() == null) continue;
                VolumeSnapshotReferenceTreeVO refTree = JSONObjectUtil.toObject(volMeta.getSnapshotReferenceTree(), VolumeSnapshotReferenceTreeVO.class);
                VolumeSnapshotReferenceTreeVO existingRefTree = dbf.findByUuid(refTree.getUuid(), VolumeSnapshotReferenceTreeVO.class);
                if (existingRefTree != null) {
                    if (!Objects.equals(existingRefTree.getRootVolumeUuid(), refTree.getRootVolumeUuid())) {
                        throw new CloudRuntimeException(String.format(
                                "VolumeSnapshotReferenceTreeVO[uuid:%s] already exists with rootVolumeUuid=%s, " +
                                        "cannot reuse for rootVolumeUuid=%s",
                                refTree.getUuid(), existingRefTree.getRootVolumeUuid(), refTree.getRootVolumeUuid()));
                    }
                    logger.debug(String.format("[MetadataRegistration] VolumeSnapshotReferenceTreeVO[uuid:%s] already exists and matches, skipping", refTree.getUuid()));
                    continue;
                }
                refTree.setPrimaryStorageUuid(ctx.primaryStorageUuid);
                refTree.setHostUuid(null);
                String newTreePath = ctx.metadataToCurrentPathMap.get(refTree.getRootInstallUrl());
                if (newTreePath != null) {
                    refTree.setRootInstallUrl(newTreePath);
                }
                refTree.setLastOpDate(now);

                logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotReferenceTreeVO[uuid:%s, rootVolumeUuid:%s, rootInstallUrl:%s]",
                        refTree.getUuid(), refTree.getRootVolumeUuid(), refTree.getRootInstallUrl()));
                dbf.persist(refTree);
                logger.debug(String.format("[MetadataRegistration] VolumeSnapshotReferenceTreeVO[uuid:%s] persisted successfully", refTree.getUuid()));
                createdVoUuids.add(refTree.getUuid());
            }
        }

        if (ctx.dto.getVolumes() != null) {
            for (VolumeResourceMetadata volMeta : ctx.dto.getVolumes()) {
                if (volMeta.getSnapshotReference() == null) continue;
                VolumeSnapshotReferenceVO ref = JSONObjectUtil.toObject(volMeta.getSnapshotReference(), VolumeSnapshotReferenceVO.class);

                boolean refExists = Q.New(VolumeSnapshotReferenceVO.class)
                        .eq(VolumeSnapshotReferenceVO_.treeUuid, ref.getTreeUuid())
                        .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, ref.getReferenceVolumeUuid())
                        .isExists();
                if (refExists) {
                    logger.debug(String.format("[MetadataRegistration] VolumeSnapshotReferenceVO[treeUuid:%s, refVolUuid:%s] already exists, skipping",
                            ref.getTreeUuid(), ref.getReferenceVolumeUuid()));
                    continue;
                }

                ref.setId(0);
                ref.setParentId(null);

                String newUrl;
                newUrl = ctx.metadataToCurrentPathMap.get(ref.getVolumeSnapshotInstallUrl());
                if (newUrl != null) ref.setVolumeSnapshotInstallUrl(newUrl);

                newUrl = ctx.metadataToCurrentPathMap.get(ref.getDirectSnapshotInstallUrl());
                if (newUrl != null) ref.setDirectSnapshotInstallUrl(newUrl);

                newUrl = ctx.metadataToCurrentPathMap.get(ref.getReferenceInstallUrl());
                if (newUrl != null) ref.setReferenceInstallUrl(newUrl);
                ref.setLastOpDate(now);

                logger.info(String.format("[MetadataRegistration] persisting VolumeSnapshotReferenceVO[treeUuid:%s, referenceVolumeUuid:%s, " +
                                "volumeSnapshotUuid:%s]",
                        ref.getTreeUuid(), ref.getReferenceVolumeUuid(), ref.getVolumeSnapshotUuid()));
                dbf.persist(ref);
                logger.debug(String.format("[MetadataRegistration] VolumeSnapshotReferenceVO[treeUuid:%s, referenceVolumeUuid:%s] persisted successfully",
                        ref.getTreeUuid(), ref.getReferenceVolumeUuid()));
            }
        }

        logger.debug(String.format("[MetadataRegistration] persistSnapshots COMPLETE for vm=%s", ctx.vmUuid));
    }

    private static void persistVolume(PersistContext ctx, VolumeResourceMetadata volMeta, VolumeVO vol, Set<String> createdVoUuids) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        logger.debug(String.format("[MetadataRegistration] persistVolume: volUuid=%s, type=%s, vmUuid=%s, " +
                        "originalInstallPath=%s",
                vol.getUuid(), vol.getType(), ctx.vmUuid, vol.getInstallPath()));

        vol.setPrimaryStorageUuid(ctx.primaryStorageUuid);
        vol.setAccountUuid(ctx.accountUuid);
        vol.setDiskOfferingUuid(null);
        vol.setRootImageUuid(null);
        String newVolPath = ctx.metadataToCurrentPathMap.get(vol.getInstallPath());
        if (newVolPath != null) {
            logger.debug(String.format("[MetadataRegistration] volume[%s] path replaced: %s -> %s",
                    vol.getUuid(), vol.getInstallPath(), newVolPath));
            vol.setInstallPath(newVolPath);
        }
        vol.setCreateDate(now);
        vol.setLastOpDate(now);
        if (vol.getStatus() == null) {
            vol.setStatus(VolumeStatus.Ready);
        }

        logger.info(String.format("[MetadataRegistration] persisting VolumeVO[uuid:%s, type:%s, vmUuid:%s, installPath:%s, size:%d]",
                vol.getUuid(), vol.getType(), vol.getVmInstanceUuid(), vol.getInstallPath(), vol.getSize()));
        dbf.persist(vol);
        logger.debug(String.format("[MetadataRegistration] VolumeVO[uuid:%s] persisted successfully", vol.getUuid()));
        createdVoUuids.add(vol.getUuid());

        logger.debug(String.format("[MetadataRegistration] calling afterVolumePersist extensions for volume[uuid:%s], psType=%s",
                vol.getUuid(), ctx.psType));
        for (VmMetadataResourcePersistExtensionPoint ext : pluginRgty.getExtensionList(VmMetadataResourcePersistExtensionPoint.class)) {
            if (ext.getPrimaryStorageType().equals(ctx.psType)) {
                ext.afterVolumePersist(ctx.primaryStorageUuid, vol.getUuid(), VolumeVO.class.getSimpleName(), ctx.hostUuid, vol.getSize(), now);
                break;
            }
        }

        logger.debug(String.format("[MetadataRegistration] restoring system tags and resource configs for volume[uuid:%s]", vol.getUuid()));
        ParsedTagsAndConfigs volParsed = ctx.parsedTagsAndConfigsMap.get(vol.getUuid());
        if (volParsed != null) {
            MetadataRegistrationUtils.persistSystemTags(dbf, volParsed.systemTags, vol.getUuid(), now);
            MetadataRegistrationUtils.persistResourceConfigs(dbf, volParsed.resourceConfigs, vol.getUuid(), now);
        }
    }

    static List<VolumeSnapshotVO> bfsOrderSnapshots(List<VolumeSnapshotVO> snaps) {
        Map<String, VolumeSnapshotVO> byUuid = new LinkedHashMap<>();
        for (VolumeSnapshotVO s : snaps) {
            byUuid.put(s.getUuid(), s);
        }

        Map<String, List<VolumeSnapshotVO>> childMap = new HashMap<>();
        List<VolumeSnapshotVO> roots = new ArrayList<>();
        for (VolumeSnapshotVO s : snaps) {
            if (s.getParentUuid() == null || !byUuid.containsKey(s.getParentUuid())) {
                roots.add(s);
            } else {
                childMap.computeIfAbsent(s.getParentUuid(), k -> new ArrayList<>()).add(s);
            }
        }

        List<VolumeSnapshotVO> result = new ArrayList<>();
        Queue<VolumeSnapshotVO> queue = new LinkedList<>(roots);
        while (!queue.isEmpty()) {
            VolumeSnapshotVO cur = queue.poll();
            result.add(cur);
            List<VolumeSnapshotVO> children = childMap.get(cur.getUuid());
            if (children != null) {
                queue.addAll(children);
            }
        }

        if (result.size() != snaps.size()) {
            Set<String> visited = result.stream().map(VolumeSnapshotVO::getUuid).collect(Collectors.toSet());
            List<String> missed = snaps.stream().map(VolumeSnapshotVO::getUuid)
                    .filter(u -> !visited.contains(u)).collect(Collectors.toList());
            throw new CloudRuntimeException(String.format(
                    "bfsOrderSnapshots: %d of %d snapshots unreachable from any root " +
                    "(snapshots with null or out-of-set parentUuid). This may indicate " +
                    "corrupted parentUuid references in metadata. missed=%s",
                    missed.size(), snaps.size(), missed));
        }

        return result;
    }

    public static void rollbackRegistration(String vmUuid) {
        logger.info(String.format("[MetadataRegistration] rollbackRegistration START: vm=%s", vmUuid));
        PluginRegistry pluginRgty = Platform.getComponentLoader().getComponent(PluginRegistry.class);

        logger.debug(String.format("[MetadataRegistration] rollback: querying volumes for vm=%s", vmUuid));
        // Must match VmMetadataBuilderUtils.collectVolumes(): include detached volumes
        // that were last attached to this VM, otherwise their snapshots/refs/tags won't
        // be cleaned up on rollback.
        // Single query to avoid race condition: two separate queries could return
        // the same volume twice if a concurrent detach commits between them.
        List<String> volUuids = SQL.New("SELECT vol.uuid FROM VolumeVO vol" +
                " WHERE vol.vmInstanceUuid = :uuid" +
                " OR (vol.vmInstanceUuid IS NULL AND vol.lastVmInstanceUuid = :uuid)", String.class)
                .param("uuid", vmUuid)
                .list();
        logger.debug(String.format("[MetadataRegistration] rollback: found %d volumes: %s", volUuids.size(), volUuids));

        if (!volUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] rollback: querying snapshots for volumes=%s", volUuids));
            // Collect snapshot UUIDs for extension rollback
            List<String> snapUuids = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid)
                    .in(VolumeSnapshotVO_.volumeUuid, volUuids).listValues();
            logger.debug(String.format("[MetadataRegistration] rollback: found %d snapshots: %s",
                    snapUuids != null ? snapUuids.size() : 0, snapUuids));

            // Call extension points to clean up storage-specific resources (e.g. LocalStorageResourceRefVO)
            List<String> allResourceUuids = new ArrayList<>();
            allResourceUuids.addAll(volUuids);
            if (snapUuids != null) {
                allResourceUuids.addAll(snapUuids);
            }
            logger.debug(String.format("[MetadataRegistration] rollback: calling afterRegistrationRollback for %d resources", allResourceUuids.size()));
            for (VmMetadataResourcePersistExtensionPoint ext :
                    pluginRgty.getExtensionList(VmMetadataResourcePersistExtensionPoint.class)) {
                ext.afterRegistrationRollback(allResourceUuids);
            }

            logger.debug(String.format("[MetadataRegistration] rollback: deleting VolumeSnapshotReferenceVO for volumes=%s", volUuids));
            SQL.New(VolumeSnapshotReferenceVO.class)
                    .in(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volUuids)
                    .hardDelete();

            logger.debug(String.format("[MetadataRegistration] rollback: querying VolumeSnapshotReferenceTreeVO for volumes=%s", volUuids));
            List<String> treeUuids = Q.New(VolumeSnapshotReferenceTreeVO.class)
                    .select(VolumeSnapshotReferenceTreeVO_.uuid)
                    .in(VolumeSnapshotReferenceTreeVO_.rootVolumeUuid, volUuids)
                    .listValues();
            if (treeUuids != null) {
                logger.debug(String.format("[MetadataRegistration] rollback: checking %d VolumeSnapshotReferenceTreeVOs for orphans", treeUuids.size()));
                for (String treeUuid : treeUuids) {
                    boolean hasOtherRefs = Q.New(VolumeSnapshotReferenceVO.class)
                            .eq(VolumeSnapshotReferenceVO_.treeUuid, treeUuid)
                            .isExists();
                    if (!hasOtherRefs) {
                        logger.debug(String.format("[MetadataRegistration] rollback: deleting orphaned VolumeSnapshotReferenceTreeVO[uuid:%s]", treeUuid));
                        SQL.New(VolumeSnapshotReferenceTreeVO.class)
                                .eq(VolumeSnapshotReferenceTreeVO_.uuid, treeUuid)
                                .hardDelete();
                    }
                }
            }

            logger.debug(String.format("[MetadataRegistration] rollback: deleting VolumeSnapshotGroupVOs for vm=%s", vmUuid));
            List<String> groupUuids = Q.New(VolumeSnapshotGroupVO.class).select(VolumeSnapshotGroupVO_.uuid).eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid).listValues();
            if (groupUuids != null && !groupUuids.isEmpty()) {
                logger.debug(String.format("[MetadataRegistration] rollback: found %d snapshot groups: %s", groupUuids.size(), groupUuids));
                SQL.New(VolumeSnapshotGroupRefVO.class).in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuids).hardDelete();
                SQL.New(VolumeSnapshotGroupVO.class).in(VolumeSnapshotGroupVO_.uuid, groupUuids).hardDelete();
            }

            logger.debug(String.format("[MetadataRegistration] rollback: deleting VolumeSnapshotVOs for volumes=%s", volUuids));
            SQL.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.volumeUuid, volUuids).hardDelete();

            logger.debug(String.format("[MetadataRegistration] rollback: deleting VolumeSnapshotTreeVOs for volumes=%s", volUuids));
            SQL.New(VolumeSnapshotTreeVO.class).in(VolumeSnapshotTreeVO_.volumeUuid, volUuids).hardDelete();
        }

        if (!volUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] rollback: deleting SystemTags and ResourceConfigs for %d volumes", volUuids.size()));
            for (String volUuid : volUuids) {
                SQL.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, volUuid).hardDelete();
                SQL.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, volUuid).hardDelete();
            }
        }

        if (!volUuids.isEmpty()) {
            logger.debug(String.format("[MetadataRegistration] rollback: deleting VolumeVOs for vm=%s", vmUuid));
            SQL.New(VolumeVO.class).in(VolumeVO_.uuid, volUuids).hardDelete();
        }

        logger.debug(String.format("[MetadataRegistration] rollback: deleting SystemTags and ResourceConfigs for vm=%s", vmUuid));
        SQL.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, vmUuid).hardDelete();
        SQL.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, vmUuid).hardDelete();

        logger.debug(String.format("[MetadataRegistration] rollback: deleting VmInstanceVO[uuid:%s]", vmUuid));
        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).hardDelete();

        try {
            if (!volUuids.isEmpty()) {
                logger.debug(String.format("[MetadataRegistration] rollback: cleaning orphaned VolumeSnapshotReferenceTreeVOs"));
                List<String> orphanTreeUuids = SQL.New("select t.uuid from VolumeSnapshotReferenceTreeVO t " +
                        "where t.rootVolumeUuid in (:volUuids) " +
                        "and t.uuid not in (select distinct r.treeUuid from VolumeSnapshotReferenceVO r where r.treeUuid is not null)"
                ).param("volUuids", volUuids).list();
                if (orphanTreeUuids != null && !orphanTreeUuids.isEmpty()) {
                    logger.debug(String.format("[MetadataRegistration] rollback: deleting %d orphaned ReferenceTreeVOs: %s",
                            orphanTreeUuids.size(), orphanTreeUuids));
                    SQL.New(VolumeSnapshotReferenceTreeVO.class).in(VolumeSnapshotReferenceTreeVO_.uuid, orphanTreeUuids).hardDelete();
                }
            }
        } catch (Exception e) {
            logger.warn(String.format("[MetadataRegistration] rollback: failed to clean up orphaned ReferenceTreeVOs: %s", e.getMessage()));
        }

        logger.info(String.format("[MetadataRegistration] rollbackRegistration COMPLETE: vm=%s", vmUuid));
    }
}
