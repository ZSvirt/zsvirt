package org.zstack.ha.header;

/**
 * @Author: DaoDao
 * @Date: 2023/4/14
 */
public enum HostHaState {
    Done,
    Setuping,
    SetupFail,
    None,
    Cancelling,
    CancelFail,
}
