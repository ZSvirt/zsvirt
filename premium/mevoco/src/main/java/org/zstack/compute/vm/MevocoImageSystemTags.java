package org.zstack.compute.vm;

import org.zstack.header.image.ImageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class MevocoImageSystemTags {
    public static String SECURITY_LEVEL_TOKEN = "securityLevel";
    public static PatternedSystemTag SECURITY_LEVEL =
            new PatternedSystemTag(String.format("securityLevel::{%s}", SECURITY_LEVEL_TOKEN), ImageVO.class);
}
