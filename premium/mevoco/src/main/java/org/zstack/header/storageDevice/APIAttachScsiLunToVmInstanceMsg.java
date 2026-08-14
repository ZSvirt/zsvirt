package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/scsi-lun/{uuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachScsiLunToVmInstanceEvent.class,
        parameterName = "params"
)
public class APIAttachScsiLunToVmInstanceMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(resourceType = ScsiLunVO.class)
    private String uuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false)
    private boolean disableMultiPathAttach = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isDisableMultiPathAttach() {
        return disableMultiPathAttach;
    }

    public void setDisableMultiPathAttach(boolean disableMultiPathAttach) {
        this.disableMultiPathAttach = disableMultiPathAttach;
    }

    public static APIAttachScsiLunToVmInstanceMsg __example__() {
        APIAttachScsiLunToVmInstanceMsg msg = new APIAttachScsiLunToVmInstanceMsg();
        msg.setUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        APIAttachScsiLunToVmInstanceMsg amsg = (APIAttachScsiLunToVmInstanceMsg) msg;
        List<APIAuditor.Result> res = new ArrayList<>();
        res.add(new APIAuditor.Result(amsg.getVmInstanceUuid(), VmInstanceVO.class));
        res.add(new APIAuditor.Result(amsg.uuid, ScsiLunVO.class));

        return res;
    }
}
