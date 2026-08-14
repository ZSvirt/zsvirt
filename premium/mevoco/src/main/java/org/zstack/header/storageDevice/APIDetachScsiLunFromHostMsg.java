package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/storage-devices/scsi-lun/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIDetachScsiLunFromHostEvent.class
)
public class APIDetachScsiLunFromHostMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = ScsiLunVO.class)
    private String uuid;

    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIDetachScsiLunFromHostMsg __example__() {
        APIDetachScsiLunFromHostMsg msg = new APIDetachScsiLunFromHostMsg();
        msg.setUuid(uuid());
        msg.setHostUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIDetachScsiLunFromHostEvent) rsp).getInventory().getUuid() : "",
                ScsiLunVO.class
        );
    }
}
