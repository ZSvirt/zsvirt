package org.zstack.storage.volume.block;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @author hanyu.liang
 * @date 2023/7/10 13:21
 */
public class GetBlockVolumeAccessPathInfoMsg extends NeedReplyMessage implements BlockVolumeMessage {
    private String uuid;

    @Override
    public String getBlockVolumeUuid() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
