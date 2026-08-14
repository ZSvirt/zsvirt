package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.APIGetChainTaskReply;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.core.APIGetChainTaskMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by MaJin on 2019/7/3.
 */

@RestRequest(
        path = "/hosts/task-details",
        method = HttpMethod.GET,
        responseClass = APIGetChainTaskReply.class
)
public class APIGetHostTaskMsg extends APIGetChainTaskMsg {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private List<String> hostUuids;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    @Override
    public List<String> getSyncSignatures() {
        List<String> syncSignatures = new ArrayList<>();
        hostUuids.forEach(hostUuid -> syncSignatures.add((HostConstant.HOST_SYNC_SIGNATURE_PREFIX + hostUuid)));
        return syncSignatures;
    }

    @Override
    public Function<String, String> getResourceUuidMaker() {
        return s -> s.substring(s.lastIndexOf("-") + 1);
    }

    public static APIGetHostTaskMsg __example__() {
        APIGetHostTaskMsg msg = new APIGetHostTaskMsg();
        msg.setHostUuids(list(uuid(HostVO.class)));
        return msg;
    }
}
