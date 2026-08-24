package org.zstack.header.errorcode;

import org.zstack.header.exception.CloudRuntimeException;

import java.util.Objects;

/**
 * Created by Wenhao.Zhang on 22/11/21
 */
public class ErrorableValue<T> {
    public final T result;
    public final ErrorCode error;

    public static <T> ErrorableValue<T> of(T result) {
        return new ErrorableValue<>(result, null);
    }

    public static <T> ErrorableValue<T> ofErrorCode(ErrorCode error) {
        return new ErrorableValue<>(null,
                Objects.requireNonNull(error, "errorCode in ErrorableValue can not be null"));
    }

    /**
     * Make sure this ErrorableValue is not success
     */
    @SuppressWarnings("unchecked")
    public <CASE> ErrorableValue<CASE> cast() {
        if (isSuccess()) {
            throw new CloudRuntimeException("Can not cast ErrorableValue");
        } else {
            return (ErrorableValue<CASE>) this;
        }
    }

    protected ErrorableValue(T result, ErrorCode error) {
        this.result = result;
        this.error = error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}
