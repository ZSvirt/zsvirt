package org.zstack.header.network.l3;

import org.zstack.header.core.Completion;

import java.util.List;

/**
 * Created by xing5 on 2016/5/20.
 */
public interface AfterAddIpRangeExtensionPoint {
    void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags);

    default boolean requiresIpRangeSynchronization(String l3NetworkUuid) {
        return false;
    }

    default void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags, Completion completion) {
        afterAddIpRange(ipr, systemTags);
        completion.success();
    }
}
