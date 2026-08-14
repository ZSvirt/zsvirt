package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * @author: kefeng.wang
 * @date: 2018-12-12
 */
@RestRequest(
        path = "/l2-networks/vxlan-pool/vni-ranges/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateVniRangeEvent.class,
        isAction = true
)
public class APIUpdateVniRangeMsg extends APIMessage implements L2NetworkMessage, APIAuditor {
    @APIParam(resourceType = VniRangeVO.class)
    private String uuid;

    @APIParam(maxLength = 255)
    private String name;

    @APINoSee
    private String l2NetworkUuid;

    public APIUpdateVniRangeMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public APIUpdateVniRangeMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIUpdateVniRangeEvent) rsp).getInventory().getUuid() : "", VniRangeVO.class);
    }

    public static APIUpdateVniRangeMsg __example__() {
        APIUpdateVniRangeMsg msg = new APIUpdateVniRangeMsg();
        msg.setUuid(uuid());
        msg.setName("VNI-NEW");
        msg.setL2NetworkUuid(uuid());
        return msg;
    }
}
