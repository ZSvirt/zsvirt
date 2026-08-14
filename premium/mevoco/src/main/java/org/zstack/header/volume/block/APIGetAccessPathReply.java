package org.zstack.header.volume.block;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * @author shenjin
 * @date 2023/6/14 10:22
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetAccessPathReply extends APIReply {
    private List<AccessPathInfo> pathInfos;

    public List<AccessPathInfo> getPathInfos() {
        return pathInfos;
    }

    public void setPathInfos(List<AccessPathInfo> pathInfos) {
        this.pathInfos = pathInfos;
    }

    public static APIGetAccessPathReply __example__() {
        APIGetAccessPathReply reply = new APIGetAccessPathReply();
        AccessPathInfo info = new AccessPathInfo();
        info.setAccessPathId(1);
        info.setAccessPathIqn("iqn");
        info.setTargetCount(1);
        reply.setPathInfos(Arrays.asList(info));
        return reply;
    }
}
