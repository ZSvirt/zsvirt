package org.zstack.portal.apimediator;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by xing5 on 2016/11/21.
 */
@TagDefinition
public class PortalSystemTags {
    public static SystemTag VALIDATION_ONLY = SystemTag.makeEphemeralTag("validationOnly");

    public static String REPLAY_ID = "replayId";
    public static SystemTag FOR_REPLAY = new SystemTag(String.format("replay::{%s}", REPLAY_ID), SystemTag.resourceTypeForEphemeralTag())
            .markAsEphemeral();
}
