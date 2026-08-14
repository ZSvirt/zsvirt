package org.zstack.header.vpc.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/vpc/hagroups/{uuid}/monitorIps",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeVpcHaGroupMonitorIpsEvent.class
)
public class APIChangeVpcHaGroupMonitorIpsMsg extends APIMessage {
    @APIParam(resourceType = VpcHaGroupVO.class)
    private String uuid;

    @APIParam(required = false, nonempty = true)
    private List<String> monitorIps;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonitorIps() {
        return monitorIps;
    }

    public void setMonitorIps(List<String> monitorIps) {
        this.monitorIps = monitorIps;
    }

    public static APIChangeVpcHaGroupMonitorIpsMsg __example__() {
        APIChangeVpcHaGroupMonitorIpsMsg msg = new APIChangeVpcHaGroupMonitorIpsMsg();
        msg.setUuid(uuid());
        msg.setMonitorIps(asList("8.8.8.8"));

        return msg;
    }
}
