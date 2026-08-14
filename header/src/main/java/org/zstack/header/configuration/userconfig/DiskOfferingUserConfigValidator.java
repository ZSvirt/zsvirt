package org.zstack.header.configuration.userconfig;

/**
 * Created by lining on 2019/5/7.
 */
public interface DiskOfferingUserConfigValidator {
    void validateDiskOfferingUserConfig(String userConfig, String diskOfferingUuid);
}