package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/18/2015.
 */
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachL3NetworkFromVmEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "NicUuidToVmUuidResolver", field = "vmNicUuid")
public class APIDetachL3NetworkFromVmMsg extends APIMessage implements VmInstanceMessage, APIMultiAuditor {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;
    @APINoSee
    private String vmInstanceUuid;

    @APINoSee
    public String l3Uuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIDetachL3NetworkFromVmMsg __example__() {
        APIDetachL3NetworkFromVmMsg msg = new APIDetachL3NetworkFromVmMsg();
        msg.vmNicUuid = uuid();
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APIDetachL3NetworkFromVmMsg amsg = (APIDetachL3NetworkFromVmMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        res.add(new APIAuditor.Result(amsg.getVmInstanceUuid(), VmInstanceVO.class));
        res.add(new APIAuditor.Result(amsg.l3Uuid, L3NetworkVO.class));

        return res;
    }
}
