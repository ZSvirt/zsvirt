package org.zstack.header.allocator;

/**
 * Created by frank on 9/17/2015.
 */
public interface ReportHostCapacityExtensionPoint {
    HostCapacityVO reportHostCapacity(HostCapacityStruct struct);
}
