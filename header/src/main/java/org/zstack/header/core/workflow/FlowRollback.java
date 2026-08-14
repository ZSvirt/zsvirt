package org.zstack.header.core.workflow;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 11/20/2015.
 */
public interface FlowRollback extends AsyncBackup {
    void rollback();

    void skipRestRollbacks();

    ErrorCode getErrorCode();
}
