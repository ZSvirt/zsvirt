package org.zstack.core.checkpoint;

public enum CheckPointState {
    Creating,
    ExecutedSuccessful,
    ExecutedFailed,
    CleanUpSuccessful,
    CleanUpFailed,
}
