package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/8.
 */

@RestRequest(
        path = "/resources/accounts",
        method = HttpMethod.GET,
        responseClass = APIGetResourceAccountReply.class
)
public class APIGetResourceAccountMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> resourceUuids;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }
 
    public static APIGetResourceAccountMsg __example__() {
        APIGetResourceAccountMsg msg = new APIGetResourceAccountMsg();
        msg.setResourceUuids(list(uuid(ResourceVO.class), uuid(VmInstanceVO.class)));
        return msg;
    }
}
