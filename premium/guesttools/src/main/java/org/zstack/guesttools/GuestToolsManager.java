package org.zstack.guesttools;

/**
 * Created by GuoYi on 2019-09-17.
 */
public interface GuestToolsManager {
    GuestToolsHypervisorBackend getGuestToolsHypervisorBackend(GuestToolsAgentType type);
}
