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

    public SensitiveTag annotation;

    public PatternedSystemTag(String tagFormat, Class resourceClass) {
        super(tagFormat, resourceClass, true);
    }

    public static PatternedSystemTag makeEphemeralTag(String tagFormatWithoutPrefix) {
        String tagFormat = Tags.prefix(EPHEMERAL_TAG_PREFIX) + tagFormatWithoutPrefix;
        PatternedSystemTag tag = new PatternedSystemTag(tagFormat, resolveEphemeralResourceClass());
        tag.markAsEphemeral();
        return tag;
    }

    private static Class resolveEphemeralResourceClass() {
        try {
            return Class.forName("org.zstack.header.tag.SystemTagVO");
        } catch (ClassNotFoundException e) {
            return Object.class;
        }
    }

    public String hideSensitiveInfo(String tag) {
        if (annotation == null) {
            return tag;
        }
        Class<? extends SensitiveTagOutputHandler> clz = annotation.customizeOutput();
        String result = tag;
        try {
            SensitiveTagOutputHandler sensitiveOutputHandler = clz.newInstance();
            result = sensitiveOutputHandler.desensitizeTag(this, tag);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("exception happened :", e);
        }
        return result;
    }
}
