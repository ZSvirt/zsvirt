package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageInventory;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/local-raid/actions",
        method = HttpMethod.PUT,
        responseClass = APIRefreshLocalRaidEvent.class,
        isAction = true
)
public class APIRefreshLocalRaidMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIRefreshLocalRaidMsg __example__() {
        APIRefreshLocalRaidMsg msg = new APIRefreshLocalRaidMsg();
        msg.setHostUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        APIRefreshLocalRaidEvent evt = (APIRefreshLocalRaidEvent) rsp;

        for (RaidControllerInventory inv : evt.getInventories()) {
            res.add(new APIAuditor.Result(inv.getUuid(), RaidControllerVO.class));
        }
        return res;
    }
}
