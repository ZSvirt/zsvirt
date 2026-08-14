package org.zstack.sns;

/**
 * Created by Qi Le on 2019-07-24
 */
public interface AfterCreateSNSApplicationEndpointExtensionPoint {
    void afterCreateSNSApplicationEndpoint(String uuid, APICreateSNSApplicationEndpointMsg msg);
}
