package org.zstack.network.l2.vxlan.vxlanNetworkPool;

/**
 * Created by weiwang on 10/03/2017.
 */
public interface L2VxlanNetworkPoolManager {
    VniAllocatorStrategy getVniAllocatorStrategy(VniAllocatorType type);
}
