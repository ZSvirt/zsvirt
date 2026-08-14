package org.zstack.externalbackup;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;

/**
 * Created by MaJin on 2019/12/4.
 */
public interface ExternalBackup {
    void handleMessage(Message msg);

    void deleteHook(Completion completion);

    /**
     * must be a idempotent function.
     * @param completion
     */
    void recoverHook(Completion completion);

    void cancelHook(String apiId, Completion completion);

    void completeHook(Completion completion);

    void initHook(Completion completion);
}
