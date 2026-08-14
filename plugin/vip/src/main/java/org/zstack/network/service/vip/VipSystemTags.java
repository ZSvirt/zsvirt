package org.zstack.network.service.vip;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by xing5 on 2016/11/19.
 */
@TagDefinition
public class VipSystemTags {
    public static SystemTag DELETE_ON_FAILURE = SystemTag.makeEphemeralTag("deleteVipOnFailure");
}
