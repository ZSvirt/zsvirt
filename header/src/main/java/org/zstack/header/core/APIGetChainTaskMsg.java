package org.zstack.header.core;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;
import java.util.function.Function;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/core/task-details",
        method = HttpMethod.GET,
        responseClass = APIGetChainTaskReply.class
)
public class APIGetChainTaskMsg extends APISyncCallMessage {
    @APIParam(nonempty = false, required = false)
    private List<String> syncSignatures;

    public void setSyncSignatures(List<String> syncSignatures) {
        this.syncSignatures = syncSignatures;
    }

    public List<String> getSyncSignatures() {
        return syncSignatures;
    }

    public Function<String, String> getResourceUuidMaker() {
        return null;
    }

    public static APIGetChainTaskMsg __example__() {
        APIGetChainTaskMsg msg = new APIGetChainTaskMsg();
        msg.setSyncSignatures(list("destroy-vm-" + uuid(VmInstanceVO.class)));
        return msg;
    }
}
