package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCodeList;

/**
 * Created by MaJin on 2021/1/26.
 */
public abstract class WhileDoneCompletion extends AbstractCompletion {
    public WhileDoneCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void done(ErrorCodeList errorCodeList);
}
