package org.zstack.portal.apimediator;

import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 * Created by xing5 on 2016/11/21.
 */
@TagDefinition
public class PortalSystemTags {
    public static SystemTag VALIDATION_ONLY = SystemTag.makeEphemeralTag("validationOnly");

    public static String REPLAY_ID = "replayId";
    public static PatternedSystemTag FOR_REPLAY = new PatternedSystemTag(String.format("replay::{%s}", REPLAY_ID), SystemTagVO.class);
}
