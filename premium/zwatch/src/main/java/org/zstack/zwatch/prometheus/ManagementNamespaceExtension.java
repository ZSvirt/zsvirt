package org.zstack.zwatch.prometheus;

import org.zstack.header.core.cloudbus.CloudBusExtensionPoint;

/**
 * Created by mingjian.deng on 2020/3/31.
 */
public class ManagementNamespaceExtension implements CloudBusExtensionPoint {
    public ManagementNamespaceExtension() {
    }

    @Override
    public void afterAddEnvelopes(String id) {
        ManagementNodePrometheusNamespace.addMsgNum();
    }
}
