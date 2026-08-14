package org.zstack.storage.device.localRaid;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/local-raid/physical-drives/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APILocateLocalRaidPhysicalDriveEvent.class,
        isAction = true
)
public class APILocateLocalRaidPhysicalDriveMsg extends APIMessage {
    @APIParam(resourceType = RaidPhysicalDriveVO.class)
    private String uuid;

    @APIParam(required = false)
    private Boolean locate = true;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getLocate() {
        return locate;
    }

    public void setLocate(Boolean locate) {
        this.locate = locate;
    }

    public static APILocateLocalRaidPhysicalDriveMsg __example__() {
        APILocateLocalRaidPhysicalDriveMsg msg = new APILocateLocalRaidPhysicalDriveMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
