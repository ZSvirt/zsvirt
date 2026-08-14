package org.zstack.header.sriov;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicVO;

/**
 * Created by GuoYi on 12/9/19.
 */
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVmNicTypeEvent.class
)
public class APIChangeVmNicTypeMsg extends APIMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    // we do not support changing vnic to vf for now
    // because none of the network services are supported on vf
    @APIParam(validValues = {"VNIC"})
    private String vmNicType;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getVmNicType() {
        return vmNicType;
    }

    public void setVmNicType(String vmNicType) {
        this.vmNicType = vmNicType;
    }

    public static APIChangeVmNicTypeMsg __example__() {
        APIChangeVmNicTypeMsg msg = new APIChangeVmNicTypeMsg();
        msg.setVmNicUuid(uuid());
        msg.setVmNicType(VmInstanceConstant.VIRTUAL_NIC_TYPE);
        return msg;
    }
}
