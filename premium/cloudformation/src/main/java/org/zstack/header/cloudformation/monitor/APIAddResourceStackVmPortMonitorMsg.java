package org.zstack.header.cloudformation.monitor;

import org.springframework.http.HttpMethod;
import org.zstack.header.cloudformation.ResourceStackVO;
import org.zstack.header.cloudformation.StackTemplateVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@TagResourceType(StackTemplateVO.class)
@RestRequest(
        path = "/cloudformation/stack/monitor/addvm",
        method = HttpMethod.POST,
        responseClass = APIAddResourceStackVmPortMonitorEvent.class,
        parameterName = "params"
)
public class APIAddResourceStackVmPortMonitorMsg extends APIMessage {
    @APIParam(resourceType = ResourceStackVO.class, required = false)
    private String stackUuid;
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    @APIParam
    private Integer port;

    public String getStackUuid() {
        return stackUuid;
    }

    public void setStackUuid(String stackUuid) {
        this.stackUuid = stackUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public static APIAddResourceStackVmPortMonitorMsg __example__() {
        APIAddResourceStackVmPortMonitorMsg msg = new APIAddResourceStackVmPortMonitorMsg();
        msg.setStackUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        msg.setPort(22);

        return msg;
    }
}
