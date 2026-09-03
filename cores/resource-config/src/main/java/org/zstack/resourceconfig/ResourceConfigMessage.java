package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;

import java.util.List;

public interface ResourceConfigMessage {
    String getName();

    default List<String> getNames() {
        return null;
    }

    String getCategory();
    default String getIdentity() {
        return GlobalConfig.produceIdentity(getCategory(), getName());
    }

    default String getIdentity(String name) {
        return GlobalConfig.produceIdentity(getCategory(), name);
    }
}
