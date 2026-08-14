package org.zstack.sns.platform.email;

import org.zstack.header.tag.TagDefinition;
import org.zstack.sns.SNSApplicationPlatformVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * author:kaicai.hu
 * Date:2019/6/10
 */
@TagDefinition
public class SNSApplicationPlatformSystemTags {
    public static String SNS_ENCRYPTTYPE_TOKEN = "encryptType";

    public static PatternedSystemTag SNS_ENCRYPTTYPE =
            new PatternedSystemTag(String.format("encryptType::{%s}", SNS_ENCRYPTTYPE_TOKEN), SNSApplicationPlatformVO.class);
}
