package org.zstack.core.validation;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface ValidationFacade {
    void validateErrorByException(Object obj);

    ErrorCode validateErrorByErrorCode(Object obj);
}
