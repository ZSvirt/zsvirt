package org.zstack.storage.volume.block;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.block.AccessPathInfo;

import java.util.List;

/**
 * @author hanyu.liang
 * @date 2023/7/10 13:21
 */
public class GetBlockVolumeAccessPathInfoReply extends MessageReply {
    List<AccessPathInfo> accessPathInfos;

    public List<AccessPathInfo> getAccessPathInfos() {
        return accessPathInfos;
    }

    public void setAccessPathInfos(List<AccessPathInfo> accessPathInfos) {
        this.accessPathInfos = accessPathInfos;
    }
}

