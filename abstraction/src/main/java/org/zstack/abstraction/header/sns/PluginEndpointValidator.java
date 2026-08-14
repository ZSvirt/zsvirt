package org.zstack.abstraction.header.sns;

import org.zstack.abstraction.header.InvalidPluginDefinitionException;
import org.zstack.abstraction.header.PluginDriver;
import org.zstack.abstraction.header.PluginValidator;

import java.util.List;
import java.util.stream.Collectors;

public class PluginEndpointValidator implements PluginValidator {
    @Override
    public Class<? extends PluginDriver> pluginClass() {
        return EndpointDriver.class;
    }

    @Override
    public void validate(PluginDriver driver) {
        if (driver.type() == null) {
            throw new InvalidPluginDefinitionException(
                String.format("Null driver type for plugin: %s", driver.getClass().getName())
            );
        }
    }

    @Override
    public void validateAllPlugins(List<PluginDriver> pluginDrivers) {
        int pluginNumber = pluginDrivers.size();

        int distinctPluginByEndpointTypeNumber = pluginDrivers.stream()
                .map(PluginDriver::type)
                .collect(Collectors.toSet())
                .size();

        if (pluginNumber == distinctPluginByEndpointTypeNumber) {
            return;
        }

        throw new InvalidPluginDefinitionException(
            String.format("Duplicate endpoint type for plugins: %s", pluginDrivers)
        );
    }
}
