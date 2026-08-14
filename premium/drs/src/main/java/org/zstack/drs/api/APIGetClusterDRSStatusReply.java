package org.zstack.drs.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.rest.SDK;
import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(fieldsTo = "all")
public class APIGetClusterDRSStatusReply extends APIReply {
    private List<HostLoad> hostLoadOverThreshold;

    public List<HostLoad> getHostLoadOverThreshold() {
        return hostLoadOverThreshold;
    }

    public void setHostLoadOverThreshold(List<HostLoad> hostLoadOverThreshold) {
        this.hostLoadOverThreshold = hostLoadOverThreshold;
    }
}

