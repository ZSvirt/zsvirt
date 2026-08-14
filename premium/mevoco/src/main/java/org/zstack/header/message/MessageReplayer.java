package org.zstack.header.message;

import org.zstack.header.core.Completion;

import java.util.function.Function;

/**
 * Created by MaJin on 2020/10/21.
 */
public interface MessageReplayer {
    long saveMessage(ReplayableMessage msg, String locationUuid, String locationType);

    void removeReplayMessage(long replayId);

    void removeResourceReplayMessage(String resourceUuid, String locationUuid);

    boolean needReplay(String locationUuid, String locationType);

    void replayMessage(String locationUuid, String locationType, Completion completion);

    <T extends ReplayableMessage> void registryMessageGroupBuilder(Class<T> msgClz, Function<T, String> groupBuilder);

    String getReplayLocationUuid(NeedReplyMessage msg);
}
