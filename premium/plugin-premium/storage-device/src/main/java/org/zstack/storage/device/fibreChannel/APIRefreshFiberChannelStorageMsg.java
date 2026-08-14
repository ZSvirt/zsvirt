package org.zstack.storage.device.fibreChannel;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.zone.ZoneVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/fiber-channel/controllers",
        method = HttpMethod.POST,
        responseClass = APIRefreshFiberChannelStorageEvent.class,
        parameterName = "params"
)
public class APIRefreshFiberChannelStorageMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(required = false, resourceType = ScsiLunVO.class)
    private List<String> scsiLunUuids;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public List<String> getScsiLunUuids() {
        return scsiLunUuids;
    }

    public void setScsiLunUuids(List<String> scsiLunUuids) {
        this.scsiLunUuids = scsiLunUuids;
    }

    public static APIRefreshFiberChannelStorageMsg __example__() {
        APIRefreshFiberChannelStorageMsg msg = new APIRefreshFiberChannelStorageMsg();
        msg.setZoneUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        APIRefreshFiberChannelStorageEvent evt = (APIRefreshFiberChannelStorageEvent) rsp;
        if (evt.getInventories() == null) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        for (FiberChannelStorageInventory inv : evt.getInventories()) {
            res.add(new APIAuditor.Result(inv.getUuid(), FiberChannelStorageVO.class));
        }
        return res;
    }
}
