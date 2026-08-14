package org.zstack.guesttools.kvm;

import org.zstack.header.core.Completion;

/**
 * Created by Wenhao.Zhang on 21/07/19
 */
public interface AfterQueryGuestToolsInfoExtensionPoint {
    void afterQueryGuestToolsInfo(QueryGuestToolsInfoContext context, Completion completion);
}
