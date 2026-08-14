package org.zstack.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by xing5 on 2016/3/28.
 */
@RestRequest(path = "/vm-instances/{uuid}/ha-levels", method = HttpMethod.POST, responseClass = APISetVmInstanceHaLevelEvent.class, parameterName = "params")
public class APISetVmInstanceHaLevelMsg extends APIMessage {

    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(validValues = { "NeverStop", "OnHostFailure", "FaultTolerance" })
    private String level;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public static APISetVmInstanceHaLevelMsg __example__() {
        APISetVmInstanceHaLevelMsg msg = new APISetVmInstanceHaLevelMsg();
        msg.setUuid("76d39c6862b840a3aa4568d83db99022");
        msg.setLevel("NeverStop");
        return msg;
    }

}
