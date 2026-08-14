package org.zstack.core.componentloader;

public interface ExtensionProcessor {
    void process(PluginExtension ext, Object[] args);
}
