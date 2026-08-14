package org.zstack.header.vm;

public enum VmInstanceStateEvent {
    /* Below event are caused by user operation */
    starting,
    stopping,
    migrating,
    migrated,
    destroying,
    rebooting,
    destroyed,
    expunging,
    pausing,
    resuming,
    volumeMigrating,
    volumeMigrated,
    volumeRecovering,
    volumeRecovered,

    /* Below events are from zstack internal logic */
    unknown,

    /* Below event are from result of user operation or vm state reported by full sync */
    running,
    stopped,
    paused,
    crashed,
    noState,
}
