package org.zstack.header.host;

/**
 * Created by Administrator on 2017-05-01.
 */
public interface AfterChangeHostStatusExtensionPoint {
    void afterChangeHostStatus(String hostUuid, HostStatus before, HostStatus next);
}
