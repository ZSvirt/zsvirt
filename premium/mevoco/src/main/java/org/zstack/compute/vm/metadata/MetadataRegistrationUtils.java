package org.zstack.compute.vm.metadata;

import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.storage.memorySnapshot.CoreMemorySnapshotConfigs;
import org.zstack.tag.SystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MetadataRegistrationUtils {
    private static final CLogger logger = Utils.getLogger(MetadataRegistrationUtils.class);

    private MetadataRegistrationUtils() {
    }

    /**
     * Decode base64-encoded system tags JSON, validate each element,
     * and return the parsed list. No persistence is done here.
     *
     * @return validated list of SystemTagVO (may be empty), or throws on bad data
     */
    public static List<SystemTagVO> decodeAndValidateSystemTags(String base64EncodedTags,
                                                                 String resourceUuid) {
        List<SystemTagVO> result = new ArrayList<>();
        if (base64EncodedTags == null || base64EncodedTags.isEmpty()) {
            return result;
        }

        String decoded = new String(Base64.getDecoder().decode(base64EncodedTags), StandardCharsets.UTF_8);
        List<?> rawList = JSONObjectUtil.toObject(decoded, ArrayList.class);
        if (rawList == null) {
            return result;
        }

        List<SystemTag> allCandidates = new ArrayList<>();
        allCandidates.addAll(CoreMemorySnapshotConfigs.restoreCandidatePatternedSystemTags);
        allCandidates.addAll(CoreMemorySnapshotConfigs.restoreCandidateSystemTags);

        for (int i = 0; i < rawList.size(); i++) {
            Object element = rawList.get(i);
            if (!(element instanceof String)) {
                throw new IllegalArgumentException(String.format(
                        "decodeAndValidateSystemTags: element[%d] is not a JSON string but %s, resource=%s",
                        i, element == null ? "null" : element.getClass().getSimpleName(), resourceUuid));
            }

            SystemTagVO tag = JSONObjectUtil.toObject((String) element, SystemTagVO.class);
            if (tag == null || tag.getTag() == null) {
                throw new IllegalArgumentException(String.format(
                        "decodeAndValidateSystemTags: element[%d] deserialized to null or missing tag field, resource=%s",
                        i, resourceUuid));
            }

            if (allCandidates.stream().noneMatch(candidate -> candidate.isMatch(tag.getTag()))) {
                logger.warn(String.format("skip restoring unrecognized system tag[tag:%s] for resource[uuid:%s], " +
                        "it may come from a newer version metadata", tag.getTag(), resourceUuid));
                continue;
            }

            result.add(tag);
        }

        return result;
    }

    /**
     * Decode base64-encoded resource configs JSON, validate each element,
     * and return the parsed list. No persistence is done here.
     *
     * @return validated list of ResourceConfigVO (may be empty), or throws on bad data
     */
    public static List<ResourceConfigVO> decodeAndValidateResourceConfigs(String base64EncodedConfigs,
                                                                          String resourceUuid) {
        List<ResourceConfigVO> result = new ArrayList<>();
        if (base64EncodedConfigs == null || base64EncodedConfigs.isEmpty()) {
            return result;
        }

        String decoded = new String(Base64.getDecoder().decode(base64EncodedConfigs), StandardCharsets.UTF_8);
        List<?> rawList = JSONObjectUtil.toObject(decoded, ArrayList.class);
        if (rawList == null) {
            return result;
        }

        for (int i = 0; i < rawList.size(); i++) {
            Object element = rawList.get(i);
            if (!(element instanceof String)) {
                throw new IllegalArgumentException(String.format(
                        "decodeAndValidateResourceConfigs: element[%d] is not a JSON string but %s, resource=%s",
                        i, element == null ? "null" : element.getClass().getSimpleName(), resourceUuid));
            }

            ResourceConfigVO config = JSONObjectUtil.toObject((String) element, ResourceConfigVO.class);
            if (config == null || config.getCategory() == null || config.getName() == null) {
                throw new IllegalArgumentException(String.format(
                        "decodeAndValidateResourceConfigs: element[%d] deserialized to null or missing category/name, resource=%s",
                        i, resourceUuid));
            }

            String identity = GlobalConfig.produceIdentity(config.getCategory(), config.getName());
            if (!CoreMemorySnapshotConfigs.resourceConfigIsRegistered(identity)) {
                logger.warn(String.format("skip restoring unrecognized resource config[category:%s, name:%s] for resource[uuid:%s], " +
                        "it may come from a newer version metadata", config.getCategory(), config.getName(), resourceUuid));
                continue;
            }

            result.add(config);
        }

        return result;
    }

    /**
     * Persist pre-decoded system tags. Each tag gets a new UUID and
     * the given resourceUuid / timestamps before being persisted.
     */
    public static void persistSystemTags(DatabaseFacade dbf, List<SystemTagVO> tags,
                                          String resourceUuid, Timestamp now) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (SystemTagVO tag : tags) {
            tag.setUuid(Platform.getUuid());
            tag.setResourceUuid(resourceUuid);
            tag.setCreateDate(now);
            tag.setLastOpDate(now);
            dbf.persist(tag);
        }
    }

    /**
     * Persist pre-decoded resource configs. Each config gets a new UUID
     * and the given resourceUuid / timestamps before being persisted.
     */
    public static void persistResourceConfigs(DatabaseFacade dbf, List<ResourceConfigVO> configs,
                                               String resourceUuid, Timestamp now) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        for (ResourceConfigVO config : configs) {
            config.setUuid(Platform.getUuid());
            config.setResourceUuid(resourceUuid);
            config.setCreateDate(now);
            config.setLastOpDate(now);
            dbf.persist(config);
        }
    }
}
