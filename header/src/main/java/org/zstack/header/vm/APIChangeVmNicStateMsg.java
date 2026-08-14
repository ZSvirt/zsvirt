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
 * Created by boce.wang on 11/09/2022.
 */
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVmNicStateEvent.class,
        isAction = true
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "NicUuidToVmUuidResolver", field = "vmNicUuid")
public class APIChangeVmNicStateMsg extends APIMessage implements VmInstanceMessage, APIMultiAuditor {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;
    @APIParam(validValues = {"enable", "disable"})
    private String state;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIChangeVmNicStateMsg __example__() {
        APIChangeVmNicStateMsg msg = new APIChangeVmNicStateMsg();
        msg.vmNicUuid = uuid();
        msg.state = "disable";
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APIChangeVmNicStateMsg amsg = (APIChangeVmNicStateMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        res.add(new APIAuditor.Result(amsg.getVmInstanceUuid(), VmInstanceVO.class));
        res.add(new APIAuditor.Result(amsg.l3Uuid, L3NetworkVO.class));

        return res;
    }
}
