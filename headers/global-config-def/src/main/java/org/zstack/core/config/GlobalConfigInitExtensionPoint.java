package org.zstack.core.config;

import java.util.List;

public interface GlobalConfigInitExtensionPoint {
    /**
     * generate global config which is hard to pre-define.
     *
     * should not write config into database,
     * should not exclude or update existing config in database,
     * GlobalConfigFacade will do it if need.
     * It should be a read-only method.
     *
     * @return all global configs which are not pre-defined.
     */
    List<GlobalConfig> getGenerationGlobalConfig();
}
