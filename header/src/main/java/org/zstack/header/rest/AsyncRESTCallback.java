package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;

public abstract class AsyncRESTCallback extends AbstractCompletion {

    public AsyncRESTCallback(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void fail(ErrorCode err);

    public abstract void success(HttpEntity<String> responseEntity);
}
