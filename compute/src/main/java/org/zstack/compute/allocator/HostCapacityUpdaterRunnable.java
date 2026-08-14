package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostCapacityVO;

/**
 * Created by frank on 11/2/2015.
 */
public interface HostCapacityUpdaterRunnable {
    HostCapacityVO call(HostCapacityVO cap);
}
