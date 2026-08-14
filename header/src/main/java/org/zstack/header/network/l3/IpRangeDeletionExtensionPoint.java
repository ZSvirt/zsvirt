package org.zstack.header.network.l3;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 11/15/2015.
 */
public interface IpRangeDeletionExtensionPoint {
    void preDeleteIpRange(IpRangeInventory ipRange);

    void beforeDeleteIpRange(IpRangeInventory ipRange);

    void afterDeleteIpRange(IpRangeInventory ipRange);

    void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode);
}
