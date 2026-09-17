package org.zstack.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by xing5 on 2016/3/29.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/ha-levels",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmInstanceHaLevelEvent.class
)
public class APIDeleteVmInstanceHaLevelMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeleteVmInstanceHaLevelMsg __example__() {
        APIDeleteVmInstanceHaLevelMsg msg = new APIDeleteVmInstanceHaLevelMsg();
        msg.setUuid("76d39c6862b840a3aa4568d83db99022");

        return msg;
    }

}
