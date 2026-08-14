package org.zstack.header.network.l3;

import java.util.List;

/**
 * Created by weiwang on 10/10/2017
 */
public interface AfterAllocateRequiredIpExtensionPoint {
    IpRangeVO afterAllocateRequiredIp(IpAllocateMessage msg, IpRangeVO allocatedIpRangeVO, List<IpRangeVO> l3IpRanges);
}
