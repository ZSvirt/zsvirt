package org.zstack.network.service;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:15 2021/10/26
 */
public class SkipApplyDecentralizedDnsException extends OperationFailureException {
    public SkipApplyDecentralizedDnsException(ErrorCode errorCode) {
        super(errorCode);
    }
}
