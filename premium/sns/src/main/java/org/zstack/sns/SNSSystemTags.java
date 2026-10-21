package org.zstack.sns;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by Qi Le on 2019-07-11
 */
@TagDefinition
public class SNSSystemTags {
    public static final String ACCESSKEY_UUID_TOKEN = "accesskey";
    public static SystemTag ACCESSKEY_UUID = new SystemTag(String.format("accesskey::{%s}", ACCESSKEY_UUID_TOKEN), SNSSmsEndpointVO.class);
}
