package org.zstack.zwatch.alarm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class EventSubscriptionSystemTags {

    public static final String CN = "cn";
    public static String LANGUAGE_TOKEN = "language";
    public static String TEXT_TOKEN = "text";
    public static PatternedSystemTag EVENT_SUBSCRIPTION_TEXT = new PatternedSystemTag(String.format("name::{%s}::{%s}", LANGUAGE_TOKEN, TEXT_TOKEN), EventSubscriptionVO.class);

    public static String NAME_TOKEN = "name";
    public static PatternedSystemTag EVENT_SUBSCRIPTION_RESOURCE_NAME = new PatternedSystemTag(String.format("resourceName::{%s}", NAME_TOKEN), EventSubscriptionVO.class);
}
