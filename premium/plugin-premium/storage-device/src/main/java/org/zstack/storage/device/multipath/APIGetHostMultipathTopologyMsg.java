package org.zstack.storage.device.multipath;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storageDevice.LunVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/storage-devices/multipath/topology",
        method = HttpMethod.GET,
        responseClass = APIGetHostMultipathTopologyReply.class
)
public class APIGetHostMultipathTopologyMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(nonempty = true, resourceType = LunVO.class)
    private List<String> lunUuids;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getLunUuids() {
        return lunUuids;
    }

    public void setLunUuids(List<String> lunUuids) {
        this.lunUuids = lunUuids;
    }

    public static APIGetHostMultipathTopologyMsg __example__() {
        APIGetHostMultipathTopologyMsg msg = new APIGetHostMultipathTopologyMsg();
        msg.setHostUuid(uuid(HostVO.class));
        msg.setLunUuids(list(uuid(LunVO.class)));
        return msg;
    }
}
