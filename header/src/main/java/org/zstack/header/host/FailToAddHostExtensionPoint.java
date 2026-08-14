package org.zstack.header.host;

/**
 * Created by xing5 on 2016/3/21.
 */
public interface FailToAddHostExtensionPoint {
    void failedToAddHost(HostInventory host, AddHostMessage msg);
}
