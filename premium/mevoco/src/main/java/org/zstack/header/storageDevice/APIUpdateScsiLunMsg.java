package org.zstack.header.storageDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/scsi-lun/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateScsiLunEvent.class
)
public class APIUpdateScsiLunMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = ScsiLunVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 256, emptyString = false)
    private String name;

    @APIParam(required = false, validValues = {"Enabled", "Disabled"})
    private String state;

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdateScsiLunMsg __example__() {
        APIUpdateScsiLunMsg msg = new APIUpdateScsiLunMsg();
        msg.setUuid(uuid());
        msg.setName("test-scsi-lun");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIUpdateScsiLunEvent) rsp).getInventory().getUuid() : "",
                ScsiLunVO.class
        );
    }
}
