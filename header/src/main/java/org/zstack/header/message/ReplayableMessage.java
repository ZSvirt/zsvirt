package org.zstack.header.message;

/**
 * Created by MaJin on 2020/10/28.
 */
public interface ReplayableMessage {
    String getResourceUuid();

    Class getReplayableClass();
}
