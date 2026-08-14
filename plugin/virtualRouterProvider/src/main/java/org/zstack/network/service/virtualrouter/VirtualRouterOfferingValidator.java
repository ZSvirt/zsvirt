package org.zstack.network.service.virtualrouter;

import org.zstack.header.errorcode.OperationFailureException;

/**
 */
public interface VirtualRouterOfferingValidator {
    void validate(VirtualRouterOfferingInventory offering) throws OperationFailureException;
}
