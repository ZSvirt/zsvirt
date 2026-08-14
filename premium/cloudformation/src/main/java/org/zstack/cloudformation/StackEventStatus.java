package org.zstack.cloudformation;

/**
 * Created by mingjian.deng on 2018/6/14.
 */
public enum StackEventStatus {
    Start,
    Finish,
    Failed,
    RollbackStart,
    RollbackFinish,
    RollbackFailed,
}
