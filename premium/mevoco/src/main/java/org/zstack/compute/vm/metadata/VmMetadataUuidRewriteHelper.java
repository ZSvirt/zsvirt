package org.zstack.compute.vm.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.metadata.ResourceMetadata;
import org.zstack.header.vm.metadata.VmInstanceMetadataDTO;
import org.zstack.header.vm.metadata.VolumeResourceMetadata;
import org.zstack.header.volume.VolumeVO;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        private RewriteResult(String originalVmUuid, String targetVmUuid, boolean changed) {
            this.originalVmUuid = originalVmUuid;
            this.targetVmUuid = targetVmUuid;
            this.changed = changed;
        }
    }

    private VmMetadataUuidRewriteHelper() {
    }

    public static RewriteResult rewriteVmUuid(VmInstanceMetadataDTO dto, boolean regenerateUuid) {
        String originalVmUuid = dto.getVm().getResourceUuid();
        if (!regenerateUuid) {
            return new RewriteResult(originalVmUuid, originalVmUuid, false);
        }

        String targetVmUuid = Platform.getUuid();
        rewriteVm(dto.getVm(), targetVmUuid);
        rewriteVolumes(dto, originalVmUuid, targetVmUuid);
        rewriteNics(dto, originalVmUuid, targetVmUuid);
        rewriteSnapshotGroups(dto, originalVmUuid, targetVmUuid);
        rewriteVmTagsAndConfigs(dto.getVm(), originalVmUuid, targetVmUuid);
        replaceRemainingVmUuidReferences(dto, originalVmUuid, targetVmUuid);
        return new RewriteResult(originalVmUuid, targetVmUuid, true);
    }

    private static void rewriteVm(ResourceMetadata vmMeta, String newVmUuid) {
        vmMeta.setResourceUuid(newVmUuid);
        VmInstanceVO vm = JSONObjectUtil.toObject(vmMeta.getVo(), VmInstanceVO.class);
        vm.setUuid(newVmUuid);
        vmMeta.setVo(JSONObjectUtil.toJsonString(vm));
    }

    private static void rewriteVolumes(VmInstanceMetadataDTO dto, String oldVmUuid, String newVmUuid) {
        if (dto.getVolumes() == null) {
            return;
        }

        for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
            VolumeVO vol = JSONObjectUtil.toObject(volMeta.getVo(), VolumeVO.class);
            if (Objects.equals(vol.getVmInstanceUuid(), oldVmUuid)) {
                vol.setVmInstanceUuid(newVmUuid);
            }
            if (Objects.equals(vol.getLastVmInstanceUuid(), oldVmUuid)) {
                vol.setLastVmInstanceUuid(newVmUuid);
            }
            volMeta.setVo(JSONObjectUtil.toJsonString(vol));
        }
    }

    private static void rewriteNics(VmInstanceMetadataDTO dto, String oldVmUuid, String newVmUuid) {
        if (dto.getNics() == null) {
            return;
        }

        for (ResourceMetadata nicMeta : dto.getNics()) {
            VmNicVO nic = JSONObjectUtil.toObject(nicMeta.getVo(), VmNicVO.class);
            if (Objects.equals(nic.getVmInstanceUuid(), oldVmUuid)) {
                nic.setVmInstanceUuid(newVmUuid);
                nicMeta.setVo(JSONObjectUtil.toJsonString(nic));
            }
        }
    }

    private static void rewriteSnapshotGroups(VmInstanceMetadataDTO dto, String oldVmUuid, String newVmUuid) {
        if (dto.getSnapshotGroups() == null) {
            return;
        }

        List<String> rewritten = new ArrayList<>();
        for (String groupJson : dto.getSnapshotGroups()) {
            VolumeSnapshotGroupVO group = JSONObjectUtil.toObject(groupJson, VolumeSnapshotGroupVO.class);
            if (Objects.equals(group.getVmInstanceUuid(), oldVmUuid)) {
                group.setVmInstanceUuid(newVmUuid);
            }
            rewritten.add(JSONObjectUtil.toJsonString(group));
        }
        dto.setSnapshotGroups(rewritten);
    }

    private static void rewriteVmTagsAndConfigs(ResourceMetadata vmMeta, String oldVmUuid, String newVmUuid) {
        vmMeta.setSystemTags(rewriteSystemTags(vmMeta.getSystemTags(), oldVmUuid, newVmUuid));
        vmMeta.setResourceConfigs(rewriteResourceConfigs(vmMeta.getResourceConfigs(), oldVmUuid, newVmUuid));
    }

    private static void replaceRemainingVmUuidReferences(VmInstanceMetadataDTO dto, String oldVmUuid, String newVmUuid) {
        replaceResourceMetadataStrings(dto.getVm(), oldVmUuid, newVmUuid);

        if (dto.getVolumes() != null) {
            for (VolumeResourceMetadata volMeta : dto.getVolumes()) {
                replaceResourceMetadataStrings(volMeta, oldVmUuid, newVmUuid);
                volMeta.setSnapshotReference(replaceUuidInJsonString(volMeta.getSnapshotReference(), oldVmUuid, newVmUuid));
                volMeta.setSnapshotReferenceTree(replaceUuidInJsonString(volMeta.getSnapshotReferenceTree(), oldVmUuid, newVmUuid));
            }
        }

        if (dto.getNics() != null) {
            for (ResourceMetadata nicMeta : dto.getNics()) {
                replaceResourceMetadataStrings(nicMeta, oldVmUuid, newVmUuid);
            }
        }

        dto.setSnapshots(replaceUuidInList(dto.getSnapshots(), oldVmUuid, newVmUuid));
        dto.setSnapshotTrees(replaceUuidInList(dto.getSnapshotTrees(), oldVmUuid, newVmUuid));
        dto.setSnapshotGroups(replaceUuidInList(dto.getSnapshotGroups(), oldVmUuid, newVmUuid));
        dto.setSnapshotGroupRefs(replaceUuidInList(dto.getSnapshotGroupRefs(), oldVmUuid, newVmUuid));
    }

    private static void replaceResourceMetadataStrings(ResourceMetadata metadata, String oldVmUuid, String newVmUuid) {
        if (metadata == null) {
            return;
        }

        metadata.setVo(replaceUuidInJsonString(metadata.getVo(), oldVmUuid, newVmUuid));
        metadata.setSystemTags(rewriteEncodedListStrings(metadata.getSystemTags(), oldVmUuid, newVmUuid));
        metadata.setResourceConfigs(rewriteEncodedListStrings(metadata.getResourceConfigs(), oldVmUuid, newVmUuid));
    }

    private static List<String> replaceUuidInList(List<String> values, String oldVmUuid, String newVmUuid) {
        if (values == null) {
            return null;
        }

        List<String> rewritten = new ArrayList<>();
        for (String value : values) {
            rewritten.add(replaceUuidInJsonString(value, oldVmUuid, newVmUuid));
        }
        return rewritten;
    }

    private static String replaceUuidInJsonString(String value, String oldVmUuid, String newVmUuid) {
        if (value == null || !value.contains(oldVmUuid)) {
            return value;
        }

        try {
            JsonElement root = new JsonParser().parse(value);
            return replaceUuidInJsonElement(null, root, oldVmUuid, newVmUuid).toString();
        } catch (Exception ignored) {
            return replaceUuid(value, oldVmUuid, newVmUuid);
        }
    }

    private static JsonElement replaceUuidInJsonElement(String fieldName, JsonElement element,
                                                       String oldVmUuid, String newVmUuid) {
        if (element == null || element.isJsonNull()) {
            return element;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                entry.setValue(replaceUuidInJsonElement(entry.getKey(), entry.getValue(), oldVmUuid, newVmUuid));
            }
            return object;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, replaceUuidInJsonElement(fieldName, array.get(i), oldVmUuid, newVmUuid));
            }
            return array;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && !isPathField(fieldName)) {
            return new JsonPrimitive(replaceUuid(element.getAsString(), oldVmUuid, newVmUuid));
        }

        return element;
    }

    private static boolean isPathField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String normalized = fieldName.toLowerCase();
        return normalized.contains("path") || PHYSICAL_INSTALL_URL_FIELDS.contains(normalized);
    }

    private static String replaceUuid(String value, String oldVmUuid, String newVmUuid) {
        return value == null ? null : value.replace(oldVmUuid, newVmUuid);
    }

    private static String rewriteSystemTags(String encodedTags, String oldVmUuid, String newVmUuid) {
        if (StringUtils.isBlank(encodedTags)) {
            return encodedTags;
        }

        List<?> rawList = decodeEncodedList(encodedTags);
        List<Object> rewritten = new ArrayList<>();
        for (Object raw : rawList) {
            if (!(raw instanceof String)) {
                rewritten.add(raw);
                continue;
            }

            SystemTagVO tag = JSONObjectUtil.toObject((String) raw, SystemTagVO.class);
            if (Objects.equals(tag.getResourceUuid(), oldVmUuid)) {
                tag.setResourceUuid(newVmUuid);
            }
            rewritten.add(JSONObjectUtil.toJsonString(tag));
        }
        return encodeList(rewritten);
    }

    private static String rewriteResourceConfigs(String encodedConfigs, String oldVmUuid, String newVmUuid) {
        if (StringUtils.isBlank(encodedConfigs)) {
            return encodedConfigs;
        }

        List<?> rawList = decodeEncodedList(encodedConfigs);
        List<Object> rewritten = new ArrayList<>();
        for (Object raw : rawList) {
            if (!(raw instanceof String)) {
                rewritten.add(raw);
                continue;
            }

            ResourceConfigVO config = JSONObjectUtil.toObject((String) raw, ResourceConfigVO.class);
            if (Objects.equals(config.getResourceUuid(), oldVmUuid)) {
                config.setResourceUuid(newVmUuid);
            }
            rewritten.add(JSONObjectUtil.toJsonString(config));
        }
        return encodeList(rewritten);
    }

    private static String rewriteEncodedListStrings(String encoded, String oldVmUuid, String newVmUuid) {
        if (StringUtils.isBlank(encoded)) {
            return encoded;
        }

        List<?> rawList = decodeEncodedList(encoded);
        List<Object> rewritten = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof String) {
                rewritten.add(replaceUuidInJsonString((String) raw, oldVmUuid, newVmUuid));
            } else {
                rewritten.add(raw);
            }
        }
        return encodeList(rewritten);
    }

    private static List<?> decodeEncodedList(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        List<?> rawList = JSONObjectUtil.toObject(decoded, ArrayList.class);
        return rawList == null ? new ArrayList<>() : rawList;
    }

    private static String encodeList(List<Object> list) {
        return Base64.getEncoder().encodeToString(JSONObjectUtil.toJsonString(list).getBytes(StandardCharsets.UTF_8));
    }
}
