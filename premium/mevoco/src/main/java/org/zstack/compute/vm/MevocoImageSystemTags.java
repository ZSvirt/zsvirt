package org.zstack.compute.vm;

import org.zstack.header.image.ImageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class MevocoImageSystemTags {
    public static String SECURITY_LEVEL_TOKEN = "securityLevel";
    public static SystemTag SECURITY_LEVEL =
            new SystemTag(String.format("securityLevel::{%s}", SECURITY_LEVEL_TOKEN), ImageVO.class);
}
