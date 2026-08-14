package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.APIGetChainTaskReply;
import org.zstack.header.core.APIGetChainTaskMsg;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/vm-instances/task-details",
        method = HttpMethod.GET,
        responseClass = APIGetChainTaskReply.class
)
public class APIGetVmTaskMsg extends APIGetChainTaskMsg {
    @APIParam(nonempty = true, resourceType = VmInstanceVO.class)
    private List<String> vmInstanceUuids;

    public void setVmInstanceUuids(List<String> vmInstanceUuids) {
        this.vmInstanceUuids = vmInstanceUuids;
    }

    public List<String> getVmInstanceUuids() {
        return vmInstanceUuids;
    }

    @Override
    public List<String> getSyncSignatures() {
        List<String> syncSignatures = new ArrayList<>();
        vmInstanceUuids.forEach(vmUuid -> syncSignatures.add((VmInstanceConstant.VM_SYNC_SIGNATURE_PREFIX + vmUuid)));
        return syncSignatures;
    }

    @Override
    public Function<String, String> getResourceUuidMaker() {
        return s -> s.substring(s.lastIndexOf("-") + 1);
    }

    public static APIGetVmTaskMsg __example__() {
        APIGetVmTaskMsg msg = new APIGetVmTaskMsg();
        msg.setVmInstanceUuids(list(uuid(VmInstanceVO.class)));
        return msg;
    }
}
