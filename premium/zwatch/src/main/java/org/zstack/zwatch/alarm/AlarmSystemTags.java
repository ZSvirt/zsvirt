package org.zstack.zwatch.alarm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class AlarmSystemTags {

    public static final String CN = "cn";

    public static String LANGUAGE_TOKEN = "language";
    public static String TEXT_TOKEN = "text";
    public static PatternedSystemTag ALARM_TEXT = new PatternedSystemTag(String.format("name::{%s}::{%s}", LANGUAGE_TOKEN, TEXT_TOKEN), AlarmVO.class);

    public static String NAME_TOKEN = "name";
    public static PatternedSystemTag ALARM_RESOURCE_NAME = new PatternedSystemTag(String.format("resourceName::{%s}", NAME_TOKEN), AlarmVO.class);
}
