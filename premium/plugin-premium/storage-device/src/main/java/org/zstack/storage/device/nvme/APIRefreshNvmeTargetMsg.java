package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storageDevice.ScsiLunVO;
import org.zstack.header.zone.ZoneVO;
import org.zstack.storage.device.iscsi.IscsiServerVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2022/8/10.
 */

@RestRequest(
        path = "/storage-devices/nvme/controllers",
        method = HttpMethod.POST,
        responseClass = APIRefreshNvmeTargetEvent.class,
        parameterName = "params"
)
public class APIRefreshNvmeTargetMsg extends APIMessage implements APIMultiAuditor {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(required = false, resourceType = NvmeLunVO.class)
    private List<String> nvmeLunUuids;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public List<String> getNvmeLunUuids() {
        return nvmeLunUuids;
    }

    public void setNvmeLunUuids(List<String> nvmeLunUuids) {
        this.nvmeLunUuids = nvmeLunUuids;
    }

    public static APIRefreshNvmeTargetMsg __example__() {
        APIRefreshNvmeTargetMsg msg = new APIRefreshNvmeTargetMsg();
        msg.setZoneUuid(uuid());
        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        APIRefreshNvmeTargetEvent evt = (APIRefreshNvmeTargetEvent) rsp;
        if (evt.getInventories() == null) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        for (NvmeTargetInventory inv : evt.getInventories()) {
            res.add(new APIAuditor.Result(inv.getUuid(), NvmeTargetVO.class));
        }
        return res;
    }
}
