package org.zstack.core.componentloader;

import java.util.ArrayList;
import java.util.List;

public interface BannedModule {
    List<String> bannedModules = new ArrayList();

    default boolean isBannedModule(String moduleName){
        if (bannedModules == null || bannedModules.isEmpty()) {
            return false;
        }
        return bannedModules.stream().anyMatch(moduleName::startsWith);
    }
}
