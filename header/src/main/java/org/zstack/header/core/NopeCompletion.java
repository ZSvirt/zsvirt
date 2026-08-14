package org.zstack.header.core;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 */
public class NopeCompletion extends Completion {
    private static final CLogger logger = Utils.getLogger(NopeCompletion.class);

    public NopeCompletion(AsyncBackup... others) {
        super(null, others);
    }


    @Override
    public void success() {
    }

    @Override
    public void fail(ErrorCode errorCode) {
        logger.warn(errorCode.getReadableDetails());
    }
}
