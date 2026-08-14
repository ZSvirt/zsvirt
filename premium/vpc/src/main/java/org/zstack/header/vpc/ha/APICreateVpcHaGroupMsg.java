package org.zstack.header.vpc.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by shixin.ruan on 20/04/2019
 */
@RestRequest(
        path = "/vpc/hagroups",
        method = HttpMethod.POST,
        responseClass = APICreateVpcHaGroupEvent.class,
        parameterName = "params"
)
public class APICreateVpcHaGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, nonempty = true)
    private List<String> monitorIps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getMonitorIps() {
        return monitorIps;
    }

    public void setMonitorIps(List<String> monitorIps) {
        this.monitorIps = monitorIps;
    }

    public static APICreateVpcHaGroupMsg __example__() {
        APICreateVpcHaGroupMsg msg = new APICreateVpcHaGroupMsg();

        msg.setName("vpcha-router");
        msg.setDescription("this is a vpc for test");
        msg.setMonitorIps(asList("8.8.8.8"));

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVpcHaGroupEvent)rsp).getInventory().getUuid() : "", VpcHaGroupVO.class);
    }
}
