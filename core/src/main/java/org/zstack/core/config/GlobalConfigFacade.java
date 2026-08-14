package org.zstack.core.config;

import java.util.Map;

public interface GlobalConfigFacade {
    @Deprecated
    String updateConfig(String category, String name, String value);

    Map<String, GlobalConfig> getAllConfig();

    <T> T getConfigValue(String category, String name, Class<T> clz);

    @Deprecated
    GlobalConfig createGlobalConfig(GlobalConfigVO vo);
}
