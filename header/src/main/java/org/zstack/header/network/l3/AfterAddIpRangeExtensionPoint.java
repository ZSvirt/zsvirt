package org.zstack.header.network.l3;

import java.util.List;

/**
 * Created by xing5 on 2016/5/20.
 */
public interface AfterAddIpRangeExtensionPoint {
    void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags);
}
