package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;


/**
 * Created by meilei007@gmail.com on 7/7/17
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmMonitorNumberEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmMonitorNumberMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam
    private Integer monitorNumber;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public Integer getMonitorNumber() {
        return monitorNumber;
    }

    public void setMonitorNumber(Integer monitorNumber) {
        this.monitorNumber = monitorNumber;
    }

    public static APISetVmMonitorNumberMsg __example__() {
        APISetVmMonitorNumberMsg msg = new APISetVmMonitorNumberMsg();
        msg.uuid = uuid();
        msg.monitorNumber = 2;
        return msg;
    }
}
