package org.zstack.header.baremetal.network;

import org.springframework.http.HttpMethod;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by GuoYi on 2019-01-03.
 */
@TagResourceType(BaremetalBondingVO.class)
@RestRequest(
        path = "/baremetal/network/bondings",
        method = HttpMethod.POST,
        responseClass = APICreateBaremetalBondingEvent.class,
        parameterName = "params"
)
public class APICreateBaremetalBondingMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String chassisUuid;

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(numberRange = {0, 6})
    private Integer mode;

    @APIParam(maxLength = BaremetalNetworkConstant.slavesMaxLength)
    private String slaves;

    @APIParam(required = false, maxLength = BaremetalNetworkConstant.optsMaxLength)
    private String opts;

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getSlaves() {
        return slaves;
    }

    public void setSlaves(String slaves) {
        this.slaves = slaves;
    }

    public String getOpts() {
        return opts;
    }

    public void setOpts(String opts) {
        this.opts = opts;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent evt) {
        return new Result(evt.isSuccess() ? ((APICreateBaremetalBondingEvent)evt).getInventory().getUuid() : "", BaremetalBondingVO.class);
    }

    public static APICreateBaremetalBondingMsg __example__() {
        APICreateBaremetalBondingMsg msg = new APICreateBaremetalBondingMsg();
        msg.setChassisUuid(uuid());
        msg.setName("bond0");
        msg.setMode(1);
        msg.setSlaves("[\"d4:ae:52:6e:d1:0d\", \"d4:ae:52:6e:d1:0e\"]");
        msg.setOpts("miimon=100");
        return msg;
    }
}
