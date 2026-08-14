package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:25 2020/7/28
 */
public class ShrinkVolumeSnapshotOnPrimaryStorageReply extends MessageReply {
    private ShrinkResult shrinkResult;

    public ShrinkResult getShrinkResult() {
        return shrinkResult;
    }

    public void setShrinkResult(ShrinkResult shrinkResult) {
        this.shrinkResult = shrinkResult;
    }
}
