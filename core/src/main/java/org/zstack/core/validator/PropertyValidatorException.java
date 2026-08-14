package org.zstack.core.validator;

import org.springframework.lang.NonNull;
import org.zstack.core.Platform;
import org.zstack.header.errorcode.ErrorCode;

public class PropertyValidatorException extends RuntimeException {
    public final ErrorCode errorCode;

    public PropertyValidatorException(String msg) {
        this(Platform.argerr("invalid property: %s", msg));
    }

    public PropertyValidatorException(@NonNull ErrorCode errorCode) {
        super(errorCode.getDetails());
        this.errorCode = errorCode;
    }
}

