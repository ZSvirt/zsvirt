package org.zstack.compute.vm.metadata.dirty;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.*;
import org.zstack.resourceconfig.APIDeleteResourceConfigMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.storage.memorySnapshot.CoreMemorySnapshotConfigs;
import org.zstack.tag.SystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Filters Tag/Config API messages, returning true only when the tag or config
 * is relevant to VM metadata (i.e. in the CoreMemorySnapshotConfigs whitelist).
 *
 * <p>Non-tag/config APIs always pass the filter (return true).</p>
 *
 * <p>This is a POJO helper — not a Spring bean. Constructed and initialized by
 * {@link VmMetadataUpdateInterceptor#start()}.</p>
 */
class MetadataRelevanceFilter {
    private static final CLogger logger = Utils.getLogger(MetadataRelevanceFilter.class);

    /**
     * Check if the API message is relevant to VM metadata.
     *
     * <p>For Tag/Config APIs, returns true only when the tag or config matches
     * the CoreMemorySnapshotConfigs whitelist. For all other APIs, returns true
     * unconditionally (no filtering needed).</p>
     */
    boolean isRelevant(APIMessage msg) {
        // Tag creation
        if (msg instanceof APICreateSystemTagMsg) {
            return isTagWhitelisted(((APICreateSystemTagMsg) msg).getTag());
        }

        // Batch tag creation — any one match is enough
        if (msg instanceof APICreateSystemTagsMsg) {
            List<String> tags = ((APICreateSystemTagsMsg) msg).getTags();
            return tags != null && tags.stream().anyMatch(this::isTagWhitelisted);
        }

        // Tag update — either old or new tag is whitelisted.
        // Note: DB lookup for old tag is unavoidable here (the msg only carries the new value).
        // This is a single-row PK query and tag-update APIs are low-frequency, so the overhead
        // in beforeDeliveryMessage is acceptable.
        if (msg instanceof APIUpdateSystemTagMsg) {
            APIUpdateSystemTagMsg m = (APIUpdateSystemTagMsg) msg;
            if (isTagWhitelisted(m.getTag())) {
                return true;
            }
            String oldTag = Q.New(SystemTagVO.class).eq(SystemTagVO_.uuid, m.getUuid()).select(SystemTagVO_.tag).findValue();
            return isTagWhitelisted(oldTag);
        }

        // Tag deletion — check the tag being deleted (single-row PK query, same rationale as above)
        if (msg instanceof APIDeleteTagMsg) {
            String tagContent = Q.New(SystemTagVO.class).eq(SystemTagVO_.uuid, ((APIDeleteTagMsg) msg).getUuid()).select(SystemTagVO_.tag).findValue();
            return isTagWhitelisted(tagContent);
        }

        // Resource config update
        if (msg instanceof APIUpdateResourceConfigMsg) {
            APIUpdateResourceConfigMsg m = (APIUpdateResourceConfigMsg) msg;
            String identity = GlobalConfig.produceIdentity(m.getCategory(), m.getName());
            return CoreMemorySnapshotConfigs.resourceConfigIsRegistered(identity);
        }

        // Resource config deletion
        if (msg instanceof APIDeleteResourceConfigMsg) {
            APIDeleteResourceConfigMsg m = (APIDeleteResourceConfigMsg) msg;
            String identity = GlobalConfig.produceIdentity(m.getCategory(), m.getName());
            return CoreMemorySnapshotConfigs.resourceConfigIsRegistered(identity);
        }

        // All other APIs — pass through
        return true;
    }

    private boolean isTagWhitelisted(String tagValue) {
        if (tagValue == null) {
            return false;
        }
        // Read directly from source lists to pick up any candidates registered after startup
        for (SystemTag candidate : CoreMemorySnapshotConfigs.restoreCandidatePatternedSystemTags) {
            if (candidate.isMatch(tagValue)) {
                return true;
            }
        }
        for (SystemTag candidate : CoreMemorySnapshotConfigs.restoreCandidateSystemTags) {
            if (candidate.isMatch(tagValue)) {
                return true;
            }
        }
        return false;
    }
}
