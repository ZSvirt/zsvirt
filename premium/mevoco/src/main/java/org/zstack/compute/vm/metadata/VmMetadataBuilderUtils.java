package org.zstack.compute.vm.metadata;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO_;
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
import org.zstack.header.vm.*;
import org.zstack.header.vm.metadata.ResourceMetadata;
import org.zstack.header.vm.metadata.VmInstanceMetadataDTO;
import org.zstack.header.vm.metadata.VmMetadataCategory;
import org.zstack.header.vm.metadata.VolumeResourceMetadata;
import org.zstack.header.vo.NoView;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.storage.memorySnapshot.CoreMemorySnapshotConfigs;
import org.zstack.tag.SystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class VmMetadataBuilderUtils {
    private static final CLogger logger = Utils.getLogger(VmMetadataBuilderUtils.class);

    private static final Gson DETERMINISTIC_GSON = new GsonBuilder()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getAnnotation(NoView.class) != null
                            || f.getAnnotation(javax.persistence.Transient.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .disableHtmlEscaping()
            .create();

    private VmMetadataBuilderUtils() {
    }

    public static VmMetadataCategory determineVmCategory(String vmInstanceUuid) {
        VmMetadataCategory category;
        if (Q.New(TemplatedVmInstanceCacheVO.class).eq(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid, vmInstanceUuid).isExists()) {
            category = VmMetadataCategory.VM_TEMPLATE_CACHE;
        } else if (Q.New(TemplatedVmInstanceVO.class).eq(TemplatedVmInstanceVO_.uuid, vmInstanceUuid).isExists()) {
            category = VmMetadataCategory.VM_TEMPLATE;
        } else {
            category = VmMetadataCategory.VM;
        }
        logger.debug(String.format("[MetadataBuilder] determineVmCategory: vm=%s -> %s", vmInstanceUuid, category));
        return category;
    }

    /**
     * Build VM metadata JSON within a single read-only transaction so that
     * all queries see a consistent DB snapshot.
     */
    public static String buildVmInstanceMetadata(DatabaseFacade dbf, String vmInstanceUuid) {
        logger.debug(String.format("[MetadataBuilder] buildVmInstanceMetadata START: vm=%s", vmInstanceUuid));

        return new SQLBatchWithReturn<String>() {
            @Override
            protected String scripts() {
                VmInstanceVO vm = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).find();
                if (vm == null) {
                    logger.warn(String.format("VM[uuid:%s] not found, skip metadata build", vmInstanceUuid));
                    return null;
                }

                List<VolumeVO> volumes = collectVolumes(vmInstanceUuid);

                // N1: Verify root volume survived the Ready/non-shareable filter.
                // During storage migration (Migrating status) or other transient states,
                // the root volume may be filtered out, producing a DTO without root disk.
                // Return null to let the flush fail gracefully and retry later.
                boolean hasRootVolume = volumes.stream().anyMatch(v -> v.getType() == VolumeType.Root);
                if (!hasRootVolume) {
                    logger.warn(String.format("[MetadataBuilder] buildVmInstanceMetadata ABORT: vm=%s, " +
                            "root volume missing after status filter (likely non-Ready state), will retry later",
                            vmInstanceUuid));
                    return null;
                }

                List<String> volumeUuids = volumes.stream().map(VolumeVO::getUuid).collect(Collectors.toList());

                List<VmNicVO> nics = q(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).list();
                nics.sort(Comparator.comparing(VmNicVO::getUuid));

                List<String> nicUuids = nics.stream().map(VmNicVO::getUuid).collect(Collectors.toList());

                List<String> allResourceUuids = new ArrayList<>();
                allResourceUuids.add(vmInstanceUuid);
                allResourceUuids.addAll(volumeUuids);
                allResourceUuids.addAll(nicUuids);

                Map<String, List<SystemTagVO>> tagsByResource = batchFetchTags(allResourceUuids);
                Map<String, List<ResourceConfigVO>> configsByResource = batchFetchConfigs(allResourceUuids);

                VmInstanceMetadataDTO dto = new VmInstanceMetadataDTO();
                dto.setSchemaVersion(dbf.getDbVersion());
                dto.setVmCategory(determineVmCategoryInTx(vmInstanceUuid));

                dto.setVm(assembleResourceMetadata(vm.getUuid(), vm,
                        tagsByResource.getOrDefault(vm.getUuid(), Collections.emptyList()),
                        filterResourceConfigs(configsByResource.getOrDefault(vm.getUuid(), Collections.emptyList()),
                                CoreMemorySnapshotConfigs.vmRestoreCandidateConfigs)));

                buildVolumeMetadata(dto, volumes, volumeUuids, tagsByResource, configsByResource);

                dto.setNics(buildNicMetadata(nics, tagsByResource, configsByResource));

                buildSnapshotMetadata(dto, vmInstanceUuid, volumeUuids);

                String json = JSONObjectUtil.toJsonString(dto);
                int payloadSize = json.getBytes(StandardCharsets.UTF_8).length;
                long rejectThreshold = VmGlobalConfig.VM_METADATA_PAYLOAD_REJECT_THRESHOLD.value(Long.class);

                logger.debug(String.format("[MetadataBuilder] buildVmInstanceMetadata: vm=%s, category=%s, " +
                                "volumes=%d, nics=%d, snapshots=%d, snapshotGroups=%d, payloadSize=%d bytes",
                        vmInstanceUuid, dto.getVmCategory(),
                        dto.getVolumes() != null ? dto.getVolumes().size() : 0,
                        dto.getNics() != null ? dto.getNics().size() : 0,
                        dto.getSnapshots() != null ? dto.getSnapshots().size() : 0,
                        dto.getSnapshotGroups() != null ? dto.getSnapshotGroups().size() : 0,
                        payloadSize));

                if (payloadSize > rejectThreshold) {
                    logger.error(String.format("VM[uuid:%s] metadata payload size %d bytes exceeds reject threshold %d bytes, " +
                            "skip metadata build", vmInstanceUuid, payloadSize, rejectThreshold));
                    return null;
                }

                return json;
            }

            private VmMetadataCategory determineVmCategoryInTx(String vmUuid) {
                VmMetadataCategory category;
                if (q(TemplatedVmInstanceCacheVO.class).eq(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid, vmUuid).isExists()) {
                    category = VmMetadataCategory.VM_TEMPLATE_CACHE;
                } else if (q(TemplatedVmInstanceVO.class).eq(TemplatedVmInstanceVO_.uuid, vmUuid).isExists()) {
                    category = VmMetadataCategory.VM_TEMPLATE;
                } else {
                    category = VmMetadataCategory.VM;
                }
                return category;
            }

            private List<VolumeVO> collectVolumes(String vmUuid) {
                // Single query to avoid race condition: two separate queries could return
                // the same volume twice if a concurrent detach commits between them.
                List<VolumeVO> allVolumes = sql("SELECT vol FROM VolumeVO vol" +
                        " WHERE vol.vmInstanceUuid = :uuid" +
                        " OR (vol.vmInstanceUuid IS NULL AND vol.lastVmInstanceUuid = :uuid)",
                        VolumeVO.class)
                        .param("uuid", vmUuid)
                        .list();

                return allVolumes.stream()
                        .filter(v -> v.getStatus() == VolumeStatus.Ready)
                        .filter(v -> !v.isShareable())
                        .sorted(Comparator.comparing(VolumeVO::getUuid))
                        .collect(Collectors.toList());
            }

            private Map<String, List<SystemTagVO>> batchFetchTags(List<String> resourceUuids) {
                if (resourceUuids.isEmpty()) {
                    return Collections.emptyMap();
                }
                List<SystemTagVO> allTags = q(SystemTagVO.class).in(SystemTagVO_.resourceUuid, resourceUuids).list();
                allTags = filterSystemTags(allTags);
                return allTags.stream().collect(Collectors.groupingBy(SystemTagVO::getResourceUuid));
            }

            private Map<String, List<ResourceConfigVO>> batchFetchConfigs(List<String> resourceUuids) {
                if (resourceUuids.isEmpty()) {
                    return Collections.emptyMap();
                }
                List<ResourceConfigVO> allConfigs = q(ResourceConfigVO.class).in(ResourceConfigVO_.resourceUuid, resourceUuids).list();
                return allConfigs.stream().collect(Collectors.groupingBy(ResourceConfigVO::getResourceUuid));
            }

            private void buildVolumeMetadata(VmInstanceMetadataDTO dto, List<VolumeVO> volumes, List<String> volumeUuids,
                                             Map<String, List<SystemTagVO>> tagsByResource,
                                             Map<String, List<ResourceConfigVO>> configsByResource) {

                Map<String, VolumeSnapshotReferenceVO> refByVolume = Collections.emptyMap();
                Map<String, VolumeSnapshotReferenceTreeVO> treeByVolume = Collections.emptyMap();

                if (!volumeUuids.isEmpty()) {
                    refByVolume = fetchSnapshotRefs(volumeUuids);
                    treeByVolume = fetchSnapshotRefTrees(refByVolume);
                }

                List<VolumeResourceMetadata> volumeMetas = new ArrayList<>();
                for (VolumeVO vol : volumes) {
                    volumeMetas.add(assembleVolumeResourceMetadata(vol,
                            tagsByResource.getOrDefault(vol.getUuid(), Collections.emptyList()),
                            filterResourceConfigs(configsByResource.getOrDefault(vol.getUuid(), Collections.emptyList()),
                                    CoreMemorySnapshotConfigs.volumeRestoreCandidateConfigs),
                            refByVolume.get(vol.getUuid()),
                            treeByVolume.get(vol.getUuid())));
                }
                dto.setVolumes(volumeMetas);
            }

            private Map<String, VolumeSnapshotReferenceVO> fetchSnapshotRefs(List<String> volumeUuids) {
                List<VolumeSnapshotReferenceVO> allRefs = q(VolumeSnapshotReferenceVO.class)
                        .in(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volumeUuids).list();
                Map<String, VolumeSnapshotReferenceVO> result = new HashMap<>();
                for (VolumeSnapshotReferenceVO ref : allRefs) {
                    result.putIfAbsent(ref.getReferenceVolumeUuid(), ref);
                }
                return result;
            }

            private Map<String, VolumeSnapshotReferenceTreeVO> fetchSnapshotRefTrees(Map<String, VolumeSnapshotReferenceVO> refByVolume) {
                Set<String> treeUuids = refByVolume.values().stream()
                        .map(VolumeSnapshotReferenceVO::getTreeUuid)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                if (treeUuids.isEmpty()) {
                    return Collections.emptyMap();
                }

                List<VolumeSnapshotReferenceTreeVO> trees = q(VolumeSnapshotReferenceTreeVO.class)
                        .in(VolumeSnapshotReferenceTreeVO_.uuid, treeUuids).list();
                Map<String, VolumeSnapshotReferenceTreeVO> treeById = new HashMap<>();
                for (VolumeSnapshotReferenceTreeVO t : trees) {
                    treeById.put(t.getUuid(), t);
                }

                Map<String, VolumeSnapshotReferenceTreeVO> treeByVolume = new HashMap<>();
                for (Map.Entry<String, VolumeSnapshotReferenceVO> entry : refByVolume.entrySet()) {
                    VolumeSnapshotReferenceVO ref = entry.getValue();
                    if (ref.getTreeUuid() != null && treeById.containsKey(ref.getTreeUuid())) {
                        treeByVolume.put(entry.getKey(), treeById.get(ref.getTreeUuid()));
                    }
                }
                return treeByVolume;
            }

            private void buildSnapshotMetadata(VmInstanceMetadataDTO dto, String vmUuid, List<String> volumeUuids) {
                if (!volumeUuids.isEmpty()) {
                    List<VolumeSnapshotVO> allSnapshots = q(VolumeSnapshotVO.class)
                            .in(VolumeSnapshotVO_.volumeUuid, volumeUuids).list();
                    allSnapshots.sort(Comparator.comparing(VolumeSnapshotVO::getUuid));
                    dto.setSnapshots(allSnapshots.stream()
                            .map(DETERMINISTIC_GSON::toJson)
                            .collect(Collectors.toList()));

                    List<VolumeSnapshotTreeVO> allTrees = q(VolumeSnapshotTreeVO.class)
                            .in(VolumeSnapshotTreeVO_.volumeUuid, volumeUuids).list();
                    allTrees.sort(Comparator.comparing(VolumeSnapshotTreeVO::getUuid));
                    dto.setSnapshotTrees(allTrees.stream()
                            .map(DETERMINISTIC_GSON::toJson)
                            .collect(Collectors.toList()));
                } else {
                    dto.setSnapshots(Collections.emptyList());
                    dto.setSnapshotTrees(Collections.emptyList());
                }

                List<VolumeSnapshotGroupVO> groups = q(VolumeSnapshotGroupVO.class)
                        .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid).list();
                groups.sort(Comparator.comparing(VolumeSnapshotGroupVO::getUuid));
                dto.setSnapshotGroups(groups.stream().map(DETERMINISTIC_GSON::toJson).collect(Collectors.toList()));

                List<String> groupUuids = groups.stream().map(VolumeSnapshotGroupVO::getUuid).collect(Collectors.toList());
                if (!groupUuids.isEmpty()) {
                    List<VolumeSnapshotGroupRefVO> refs = q(VolumeSnapshotGroupRefVO.class)
                            .in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuids).list();
                    refs.sort(Comparator.comparing(VolumeSnapshotGroupRefVO::getVolumeSnapshotGroupUuid)
                            .thenComparing(VolumeSnapshotGroupRefVO::getVolumeUuid));
                    dto.setSnapshotGroupRefs(refs.stream()
                            .map(DETERMINISTIC_GSON::toJson).collect(Collectors.toList()));
                } else {
                    dto.setSnapshotGroupRefs(Collections.emptyList());
                }
            }
        }.execute();
    }

    private static List<ResourceMetadata> buildNicMetadata(List<VmNicVO> nics, Map<String, List<SystemTagVO>> tagsByResource,
                                                           Map<String, List<ResourceConfigVO>> configsByResource) {

        List<ResourceMetadata> nicMetas = new ArrayList<>();
        for (VmNicVO n : nics) {
            nicMetas.add(assembleResourceMetadata(n.getUuid(), n,
                    tagsByResource.getOrDefault(n.getUuid(), Collections.emptyList()),
                    filterResourceConfigs(configsByResource.getOrDefault(n.getUuid(), Collections.emptyList()),
                            CoreMemorySnapshotConfigs.vmNicRestoreCandidateConfigs)));
        }
        return nicMetas;
    }

    private static VolumeResourceMetadata assembleVolumeResourceMetadata(VolumeVO vol, List<SystemTagVO> tags, List<ResourceConfigVO> configs,
                                                                         VolumeSnapshotReferenceVO ref,
                                                                         VolumeSnapshotReferenceTreeVO tree) {
        VolumeResourceMetadata meta = new VolumeResourceMetadata();
        meta.setResourceUuid(vol.getUuid());
        meta.setVo(DETERMINISTIC_GSON.toJson(vol));

        tags = new ArrayList<>(tags);
        tags.sort(Comparator.comparing(SystemTagVO::getUuid));
        List<String> tagJsons = tags.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
        meta.setSystemTags(Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(tagJsons).getBytes(StandardCharsets.UTF_8)));

        configs = new ArrayList<>(configs);
        configs.sort(Comparator.comparing(ResourceConfigVO::getUuid));
        List<String> cfgJsons = configs.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
        meta.setResourceConfigs(Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(cfgJsons).getBytes(StandardCharsets.UTF_8)));

        if (ref != null) {
            meta.setSnapshotReference(JSONObjectUtil.toJsonString(ref));
        }

        if (tree != null) {
            meta.setSnapshotReferenceTree(JSONObjectUtil.toJsonString(tree));
        }

        return meta;
    }

    private static ResourceMetadata assembleResourceMetadata(String resourceUuid, Object vo, List<SystemTagVO> tags, List<ResourceConfigVO> configs) {
        ResourceMetadata meta = new ResourceMetadata();
        meta.setResourceUuid(resourceUuid);
        meta.setVo(DETERMINISTIC_GSON.toJson(vo));

        tags = new ArrayList<>(tags);
        tags.sort(Comparator.comparing(SystemTagVO::getUuid));
        List<String> tagJsons = tags.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
        meta.setSystemTags(Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(tagJsons).getBytes(StandardCharsets.UTF_8)));

        configs = new ArrayList<>(configs);
        configs.sort(Comparator.comparing(ResourceConfigVO::getUuid));
        List<String> cfgJsons = configs.stream().map(JSONObjectUtil::toJsonString).collect(Collectors.toList());
        meta.setResourceConfigs(Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(cfgJsons).getBytes(StandardCharsets.UTF_8)));

        return meta;
    }

    private static List<SystemTagVO> filterSystemTags(List<SystemTagVO> tagVOs) {
        List<SystemTag> allCandidates = new ArrayList<>();
        allCandidates.addAll(CoreMemorySnapshotConfigs.restoreCandidatePatternedSystemTags);
        allCandidates.addAll(CoreMemorySnapshotConfigs.restoreCandidateSystemTags);

        return tagVOs.stream()
                .filter(vo -> allCandidates.stream().anyMatch(candidate -> candidate.isMatch(vo.getTag())))
                .collect(Collectors.toList());
    }

    private static List<ResourceConfigVO> filterResourceConfigs(List<ResourceConfigVO> cfgVOs, List<GlobalConfig> candidateConfigs) {
        Set<String> allowedIdentities = candidateConfigs.stream()
                .map(GlobalConfig::getIdentity)
                .collect(Collectors.toSet());

        return cfgVOs.stream()
                .filter(vo -> allowedIdentities.contains(
                        GlobalConfig.produceIdentity(vo.getCategory(), vo.getName())))
                .collect(Collectors.toList());
    }
}
