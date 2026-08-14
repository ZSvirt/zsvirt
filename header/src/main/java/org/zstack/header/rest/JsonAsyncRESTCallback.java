package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;

public abstract class JsonAsyncRESTCallback<T> extends AsyncRESTCallback {
    public JsonAsyncRESTCallback(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void fail(ErrorCode err);

    public abstract void success(T ret);

    public abstract Class<T> getReturnClass();

    @Override
    public final void success(HttpEntity<String> entity) {
        throw new CloudRuntimeException("this method should not be called");
    }
}
