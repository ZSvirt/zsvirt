package org.zstack.abstraction.header;

import java.util.List;

public interface PluginValidator {
    Class<? extends PluginDriver> pluginClass();

    void validate(PluginDriver driver);

    void validateAllPlugins(List<PluginDriver> pluginDrivers);
}
