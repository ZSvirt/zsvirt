package org.zstack.compute.host;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;

public interface HostReconnectTaskFactory {
    HostReconnectTask createTask(String uuid, NoErrorCompletion completion);

    default HostReconnectTask createTaskWithLastConnectError(String uuid,
                                                             ErrorCode errorCode,
                                                             NoErrorCompletion completion) {
        return createTask(uuid, completion);
    }

    String getHypervisorType();
}
