package org.zstack.core.config;

public interface ResourceConfigGetter {
    <T> T getResourceConfigValue(GlobalConfig gc, String resourceUuid, Class<T> clz);
}
