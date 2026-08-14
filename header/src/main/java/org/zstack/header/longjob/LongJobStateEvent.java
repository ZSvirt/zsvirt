package org.zstack.header.longjob;

/**
 * Created by MaJin on 2019/10/11.
 */
public enum LongJobStateEvent {
    start,
    succeed,
    fail,
    resume,
    suspend,
    canceling,
    canceled
}
