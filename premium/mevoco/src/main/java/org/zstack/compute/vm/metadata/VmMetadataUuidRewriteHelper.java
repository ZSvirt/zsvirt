package org.zstack.compute.vm.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.reference.VolumeSnapshotReferenceTreeVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.metadata.ResourceMetadata;
import org.zstack.header.vm.metadata.VmInstanceMetadataDTO;
import org.zstack.header.vm.metadata.VolumeResourceMetadata;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VmMetadataUuidRewriteHelper {
    private static final List<String> PHYSICAL_INSTALL_URL_FIELDS = Arrays.asList(
            "rootinstallurl",
            "volumesnapshotinstallurl",
            "directsnapshotinstallurl",
            "referenceinstallurl"
    );

    public static class RewriteResult {
        public final String originalVmUuid;
        public final String targetVmUuid;
        public final boolean changed;
        public final Map<String, String> uuidMapping;

        private RewriteResult(String originalVmUuid, String targetVmUuid, boolean changed,
                              Map<String, String> uuidMapping) {
            this.originalVmUuid = originalVmUuid;
            this.targetVmUuid = targetVmUuid;
            this.changed = changed;
            this.uuidMapping = Collections.unmodifiableMap(new LinkedHashMap<>(uuidMapping));
        }
    }

    private VmMetadataUuidRewriteHelper() {
    }

    public static RewriteResult rewriteVmUuid(VmInstanceMetadataDTO dto, boolean regenerateUuid) {
        return rewriteVmUuid(dto, regenerateUuid, VmMetadataUuidAllocator.databaseBacked());
    }

    public static RewriteResult rewriteVmUuid(VmInstanceMetadataDTO dto, boolean regenerateUuid,
                                               VmMetadataUuidAllocator allocator) {
        String originalVmUuid = dto.getVm().getResourceUuid();
        if (!regenerateUuid) {
            return new RewriteResult(originalVmUuid, originalVmUuid, false, Collections.emptyMap());
        }
        if (allocator == null) {
            throw new IllegalArgumentException("allocator cannot be null");
        }

        Map<String, VmMetadataUuidAllocator.ResourceType> originalResources = collectResources(dto);
        allocator.reserveAll(originalResources.keySet());

        Map<String, String> uuidMapping = new LinkedHashMap<>();
        for (Map.Entry<String, VmMetadataUuidAllocator.ResourceType> entry : originalResources.entrySet()) {
            uuidMapping.put(entry.getKey(), allocator.allocate(entry.getValue()));
        }

        rewriteVm(dto.getVm(), uuidMapping);
        rewriteVolumes(dto, uuidMapping);
        dto.setSnapshots(rewriteSnapshots(dto.getSnapshots(), uuidMapping));
        dto.setSnapshotTrees(rewriteSnapshotTrees(dto.getSnapshotTrees(), uuidMapping));
        dto.setSnapshotGroups(rewriteSnapshotGroups(dto.getSnapshotGroups(), uuidMapping));
        dto.setSnapshotGroupRefs(rewriteJsonList(dto.getSnapshotGroupRefs(), uuidMapping));

        String targetVmUuid = uuidMapping.get(originalVmUuid);
        return new RewriteResult(originalVmUuid, targetVmUuid, true, uuidMapping);
    }

    private static Map<String, VmMetadataUuidAllocator.ResourceType> collectResources(VmInstanceMetadataDTO dto) {
        Map<String, VmMetadataUuidAllocator.ResourceType> resources = new LinkedHashMap<>();
        addResource(resources, dto.getVm().getResourceUuid(), VmMetadataUuidAllocator.ResourceType.VM);

        if (dto.getVolumes() != null) {
            for (VolumeResourceMetadata volumeMeta : dto.getVolumes()) {
                VolumeVO volume = JSONObjectUtil.toObject(volumeMeta.getVo(), VolumeVO.class);
                addResource(resources, volume.getUuid(), VmMetadataUuidAllocator.ResourceType.VOLUME);

                if (volumeMeta.getSnapshotReferenceTree() != null) {
                    VolumeSnapshotReferenceTreeVO referenceTree = JSONObjectUtil.toObject(
                            volumeMeta.getSnapshotReferenceTree(), VolumeSnapshotReferenceTreeVO.class);
                    addResource(resources, referenceTree.getUuid(),
                            VmMetadataUuidAllocator.ResourceType.SNAPSHOT_REFERENCE_TREE);
                }
            }
        }

        if (dto.getSnapshots() != null) {
            for (String snapshotJson : dto.getSnapshots()) {
                VolumeSnapshotVO snapshot = JSONObjectUtil.toObject(snapshotJson, VolumeSnapshotVO.class);
                addResource(resources, snapshot.getUuid(), VmMetadataUuidAllocator.ResourceType.SNAPSHOT);
                // The persist path can reconstruct a snapshot tree from snapshot.treeUuid when
                // snapshotTrees is absent, so that referenced tree is an independent identity too.
                addResource(resources, snapshot.getTreeUuid(),
                        VmMetadataUuidAllocator.ResourceType.SNAPSHOT_TREE);
            }
        }

        if (dto.getSnapshotTrees() != null) {
            for (String treeJson : dto.getSnapshotTrees()) {
                addResource(resources,
                        JSONObjectUtil.toObject(treeJson, VolumeSnapshotTreeVO.class).getUuid(),
                        VmMetadataUuidAllocator.ResourceType.SNAPSHOT_TREE);
            }
        }

        if (dto.getSnapshotGroups() != null) {
            for (String groupJson : dto.getSnapshotGroups()) {
                addResource(resources,
                        JSONObjectUtil.toObject(groupJson, VolumeSnapshotGroupVO.class).getUuid(),
                        VmMetadataUuidAllocator.ResourceType.SNAPSHOT_GROUP);
            }
        }

        if (dto.getSnapshotGroupRefs() != null) {
            for (String refJson : dto.getSnapshotGroupRefs()) {
                addResource(resources,
                        JSONObjectUtil.toObject(refJson, VolumeSnapshotGroupRefVO.class).getVolumeSnapshotUuid(),
                        VmMetadataUuidAllocator.ResourceType.SNAPSHOT);
            }
        }

        return resources;
    }

    private static void addResource(Map<String, VmMetadataUuidAllocator.ResourceType> resources,
                                    String uuid, VmMetadataUuidAllocator.ResourceType resourceType) {
        if (StringUtils.isNotBlank(uuid)) {
            resources.putIfAbsent(uuid, resourceType);
        }
    }

    private static void rewriteVm(ResourceMetadata vmMeta, Map<String, String> uuidMapping) {
        String newVmUuid = uuidMapping.get(vmMeta.getResourceUuid());
        vmMeta.setResourceUuid(newVmUuid);

        VmInstanceVO vm = JSONObjectUtil.toObject(vmMeta.getVo(), VmInstanceVO.class);
        vm.setUuid(newVmUuid);
        vmMeta.setVo(rewriteJsonString(JSONObjectUtil.toJsonString(vm), uuidMapping));
        rewriteTagsAndConfigs(vmMeta, uuidMapping);
    }

    private static void rewriteVolumes(VmInstanceMetadataDTO dto, Map<String, String> uuidMapping) {
        if (dto.getVolumes() == null) {
            return;
        }

        for (VolumeResourceMetadata volumeMeta : dto.getVolumes()) {
            VolumeVO volume = JSONObjectUtil.toObject(volumeMeta.getVo(), VolumeVO.class);
            String newVolumeUuid = uuidMapping.get(volume.getUuid());
            volume.setUuid(newVolumeUuid);
            volumeMeta.setResourceUuid(newVolumeUuid);
            volumeMeta.setVo(rewriteJsonString(JSONObjectUtil.toJsonString(volume), uuidMapping));
            rewriteTagsAndConfigs(volumeMeta, uuidMapping);

            if (volumeMeta.getSnapshotReferenceTree() != null) {
                VolumeSnapshotReferenceTreeVO referenceTree = JSONObjectUtil.toObject(
                        volumeMeta.getSnapshotReferenceTree(), VolumeSnapshotReferenceTreeVO.class);
                referenceTree.setUuid(uuidMapping.get(referenceTree.getUuid()));
                volumeMeta.setSnapshotReferenceTree(
                        rewriteJsonString(JSONObjectUtil.toJsonString(referenceTree), uuidMapping));
            }
            volumeMeta.setSnapshotReference(
                    rewriteJsonString(volumeMeta.getSnapshotReference(), uuidMapping));
        }
    }

    private static List<String> rewriteSnapshots(List<String> snapshots, Map<String, String> uuidMapping) {
        if (snapshots == null) {
            return null;
        }

        List<String> rewritten = new ArrayList<>();
        for (String snapshotJson : snapshots) {
            VolumeSnapshotVO snapshot = JSONObjectUtil.toObject(snapshotJson, VolumeSnapshotVO.class);
            snapshot.setUuid(uuidMapping.get(snapshot.getUuid()));
            rewritten.add(rewriteJsonString(JSONObjectUtil.toJsonString(snapshot), uuidMapping));
        }
        return rewritten;
    }

    private static List<String> rewriteSnapshotTrees(List<String> snapshotTrees,
                                                     Map<String, String> uuidMapping) {
        if (snapshotTrees == null) {
            return null;
        }

        List<String> rewritten = new ArrayList<>();
        for (String treeJson : snapshotTrees) {
            VolumeSnapshotTreeVO tree = JSONObjectUtil.toObject(treeJson, VolumeSnapshotTreeVO.class);
            tree.setUuid(uuidMapping.get(tree.getUuid()));
            rewritten.add(rewriteJsonString(JSONObjectUtil.toJsonString(tree), uuidMapping));
        }
        return rewritten;
    }

    private static List<String> rewriteSnapshotGroups(List<String> snapshotGroups,
                                                      Map<String, String> uuidMapping) {
        if (snapshotGroups == null) {
            return null;
        }

        List<String> rewritten = new ArrayList<>();
        for (String groupJson : snapshotGroups) {
            VolumeSnapshotGroupVO group = JSONObjectUtil.toObject(groupJson, VolumeSnapshotGroupVO.class);
            group.setUuid(uuidMapping.get(group.getUuid()));
            rewritten.add(rewriteJsonString(JSONObjectUtil.toJsonString(group), uuidMapping));
        }
        return rewritten;
    }

    private static void rewriteTagsAndConfigs(ResourceMetadata metadata, Map<String, String> uuidMapping) {
        metadata.setSystemTags(rewriteEncodedListStrings(metadata.getSystemTags(), uuidMapping));
        metadata.setResourceConfigs(rewriteEncodedListStrings(metadata.getResourceConfigs(), uuidMapping));
    }

    private static List<String> rewriteJsonList(List<String> values, Map<String, String> uuidMapping) {
        if (values == null) {
            return null;
        }

        List<String> rewritten = new ArrayList<>();
        for (String value : values) {
            rewritten.add(rewriteJsonString(value, uuidMapping));
        }
        return rewritten;
    }

    private static String rewriteJsonString(String value, Map<String, String> uuidMapping) {
        if (value == null || !containsMappedUuid(value, uuidMapping)) {
            return value;
        }

        try {
            JsonElement root = new JsonParser().parse(value);
            return rewriteJsonElement(null, root, uuidMapping).toString();
        } catch (Exception ignored) {
            // Preserve the previous helper's support for metadata fields that are plain strings
            // rather than JSON documents.
            return rewriteMappedUuids(value, uuidMapping);
        }
    }

    private static JsonElement rewriteJsonElement(String fieldName, JsonElement element,
                                                  Map<String, String> uuidMapping) {
        if (element == null || element.isJsonNull()) {
            return element;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                entry.setValue(rewriteJsonElement(entry.getKey(), entry.getValue(), uuidMapping));
            }
            return object;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, rewriteJsonElement(fieldName, array.get(i), uuidMapping));
            }
            return array;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && !isPathField(fieldName)) {
            return new JsonPrimitive(rewriteMappedUuids(element.getAsString(), uuidMapping));
        }

        return element;
    }

    private static boolean containsMappedUuid(String value, Map<String, String> uuidMapping) {
        for (String originalUuid : uuidMapping.keySet()) {
            if (value.contains(originalUuid)) {
                return true;
            }
        }
        return false;
    }

    private static String rewriteMappedUuids(String value, Map<String, String> uuidMapping) {
        String rewritten = value;
        for (Map.Entry<String, String> entry : uuidMapping.entrySet()) {
            rewritten = rewritten.replace(entry.getKey(), entry.getValue());
        }
        return rewritten;
    }

    private static boolean isPathField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String normalized = fieldName.toLowerCase();
        return normalized.contains("path") || PHYSICAL_INSTALL_URL_FIELDS.contains(normalized);
    }

    private static String rewriteEncodedListStrings(String encoded, Map<String, String> uuidMapping) {
        if (StringUtils.isBlank(encoded)) {
            return encoded;
        }

        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        List<?> rawList = JSONObjectUtil.toObject(decoded, ArrayList.class);
        List<Object> rewritten = new ArrayList<>();
        if (rawList != null) {
            for (Object raw : rawList) {
                if (raw instanceof String) {
                    rewritten.add(rewriteJsonString((String) raw, uuidMapping));
                } else {
                    rewritten.add(raw);
                }
            }
        }
        return Base64.getEncoder().encodeToString(
                JSONObjectUtil.toJsonString(rewritten).getBytes(StandardCharsets.UTF_8));
    }
}
