package org.zstack.header.configuration.userconfig;

import java.util.Map;

/**
 * Created by lining on 2019/4/17.
 */
public class InstanceOfferingDisplayAttributeConfig {
    private Map<String, String> rootVolume;

    public Map<String, String> getRootVolume() {
        return rootVolume;
    }

    public void setRootVolume(Map<String, String> rootVolume) {
        this.rootVolume = rootVolume;
    }
}
