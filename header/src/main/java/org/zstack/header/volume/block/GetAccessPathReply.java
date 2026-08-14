package org.zstack.header.volume.block;

import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.block.AccessPathInfo;

import java.util.List;

/**
 * @author shenjin
 * @date 2023/6/18 16:11
 */
public class GetAccessPathReply extends MessageReply {
    private List<AccessPathInfo> infos;

    public List<AccessPathInfo> getInfos() {
        return infos;
    }

    public void setInfos(List<AccessPathInfo> infos) {
        this.infos = infos;
    }
}
