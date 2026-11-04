package org.zstack.header.tag;

import org.zstack.header.configuration.ConfigAO;

public interface TagConstant {
    String SERVICE_ID = "tag";

    String ACTION_CATEGORY = "tag";

    String EPHEMERAL_TAG_PREFIX = "ephemeral";

    String RESOURCE_CONFIG_TAG_PREFIX = ConfigAO.RESOURCE_CONFIG_TAG_PREFIX;
    String RESOURCE_CONFIG_CATEGORY_TOKEN = ConfigAO.RESOURCE_CONFIG_CATEGORY_TOKEN;
    String RESOURCE_CONFIG_NAME_TOKEN = ConfigAO.RESOURCE_CONFIG_NAME_TOKEN;
    String RESOURCE_CONFIG_VALUE_TOKEN = ConfigAO.RESOURCE_CONFIG_VALUE_TOKEN;
}
