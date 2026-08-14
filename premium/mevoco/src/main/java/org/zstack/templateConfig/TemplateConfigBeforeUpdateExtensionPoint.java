package org.zstack.templateConfig;

public interface TemplateConfigBeforeUpdateExtensionPoint {
    void beforeUpdateExtensionPoint(TemplateConfig oldConfig, String newValue);
}
