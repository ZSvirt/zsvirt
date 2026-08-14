package org.zstack.templateConfig;

public interface TemplateConfigUpdateExtensionPoint {
    void updateTemplateConfig(TemplateConfig oldConfig, TemplateConfig newConfig);
}
