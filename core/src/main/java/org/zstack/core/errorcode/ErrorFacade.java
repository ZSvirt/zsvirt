package org.zstack.core.errorcode;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;

import java.util.List;

/**
 */
public interface ErrorFacade {
    /**
     * @return a sorted copy of all error codes loaded from classpath {@code errorCodes/*.xml}
     */
    List<ErrorCode> getAllErrorCodes();

    ErrorCode instantiateErrorCode(Enum code, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, ErrorCode cause);

    ErrorCode instantiateErrorCode(Enum code, String details, ErrorCode cause);

    ErrorCode instantiateErrorCode(String code, String details, ErrorCode cause);

    ErrorCode stringToOperationError(String details, ErrorCode cause);

    ErrorCode instantiateErrorCode(Enum code, List<ErrorCode> causes);

    ErrorCode instantiateErrorCode(String code, List<ErrorCode> causes);

    ErrorCode instantiateErrorCode(Enum code, String details, List<ErrorCode> causes);

    ErrorCode instantiateErrorCode(String code, String details, List<ErrorCode> causes);

    ErrorCode stringToOperationError(String details, List<ErrorCode> causes);

    ErrorCode instantiateErrorCode(Enum<?> code, String fmt, Object... args);

    ErrorCode instantiateErrorCode(String code, String fmt, Object... args);

    ErrorCode stringToInternalError(String details);

    ErrorCode throwableToInternalError(Throwable t);

    ErrorCode stringToTimeoutError(String details);

    ErrorCode throwableToTimeoutError(Throwable t);

    ErrorCode stringToOperationError(String details);

    ErrorCode throwableToOperationError(Throwable t);

    ErrorCode stringToInvalidArgumentError(String details);

    ErrorCode throwableToInvalidArgumentError(Throwable t);
}
