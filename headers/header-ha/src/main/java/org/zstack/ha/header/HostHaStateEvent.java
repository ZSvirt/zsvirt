package org.zstack.ha.header;

public enum HostHaStateEvent {
    setup,
    setupDone,
    setupFail,
    cancel,
    cancelDone,
    cancelFail,
    syncExist,
    syncNotExist,
}
