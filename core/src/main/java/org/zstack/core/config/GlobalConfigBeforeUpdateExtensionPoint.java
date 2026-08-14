package org.zstack.core.config;

/**
 * Created by kayo on 2018/9/13.
 */
public interface GlobalConfigBeforeUpdateExtensionPoint {
    void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue);
}
