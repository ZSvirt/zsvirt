package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.ShrinkResult;

public class ShrinkVolumeOnPrimaryStorageReply extends MessageReply {
    private ShrinkResult shrinkResult;

    public ShrinkResult getShrinkResult() {
        return shrinkResult;
    }

    public void setShrinkResult(ShrinkResult shrinkResult) {
        this.shrinkResult = shrinkResult;
    }
}
