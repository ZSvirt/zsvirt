package org.zstack.zwatch.alarm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class AlarmSystemTags {

    public static final String CN = "cn";

    public static String LANGUAGE_TOKEN = "language";
    public static String TEXT_TOKEN = "text";
    public static SystemTag ALARM_TEXT = new SystemTag(String.format("name::{%s}::{%s}", LANGUAGE_TOKEN, TEXT_TOKEN), AlarmVO.class);

    public static String NAME_TOKEN = "name";
    public static SystemTag ALARM_RESOURCE_NAME = new SystemTag(String.format("resourceName::{%s}", NAME_TOKEN), AlarmVO.class);
}
