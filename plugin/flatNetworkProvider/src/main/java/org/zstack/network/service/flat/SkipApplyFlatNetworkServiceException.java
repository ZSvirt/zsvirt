package org.zstack.network.service.flat;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:15 2021/10/26
 */
public class SkipApplyFlatNetworkServiceException extends OperationFailureException {
    public SkipApplyFlatNetworkServiceException(ErrorCode errorCode) {
        super(errorCode);
    }
}
