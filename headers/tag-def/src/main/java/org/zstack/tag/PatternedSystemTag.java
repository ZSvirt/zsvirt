package org.zstack.tag;

import org.zstack.utils.Tags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @deprecated Prefer {@link SystemTag} with {@code hasPattern=true}. Kept so existing
 * {@code new PatternedSystemTag(...)} definition sites compile unchanged.
 */
@Deprecated
public class PatternedSystemTag extends SystemTag {
    private static final CLogger logger = Utils.getLogger(PatternedSystemTag.class);

    public PatternedSystemTag(String tagFormat, Class<?> resourceClass) {
        super(tagFormat, resourceClass);
    }

    /**
     * use {@link SystemTag#makeEphemeralTag(String)}
     */
    @Deprecated
    public static PatternedSystemTag makeEphemeralTag(String tagFormatWithoutPrefix) {
        String tagFormat = Tags.prefix(EPHEMERAL_TAG_PREFIX) + tagFormatWithoutPrefix;
        PatternedSystemTag tag = new PatternedSystemTag(tagFormat, resourceTypeForEphemeralTag());
        tag.markAsEphemeral();
        return tag;
    }
}
