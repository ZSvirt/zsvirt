package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCodeList;

/**
 * Created by MaJin on 2021/1/26.
 */
public class NopeWhileDoneCompletion extends WhileDoneCompletion {
    public NopeWhileDoneCompletion(AsyncBackup... others) {
        super(null, others);
    }

    public NopeWhileDoneCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    @Override
    public void done(ErrorCodeList errorCodeList) {}
}
