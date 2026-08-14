package org.zstack.header.configuration.userconfig;

/**
 * Created by lining on 2019/5/7.
 */
public interface InstanceOfferingUserConfigValidator {
    void validateInstanceOfferingUserConfig(String userConfig, String instanceOfferingUuid);
}
