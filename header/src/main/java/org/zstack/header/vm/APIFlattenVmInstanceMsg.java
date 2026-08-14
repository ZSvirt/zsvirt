package org.zstack.header.vm;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIFlattenVmInstanceEvent.class,
        isAction = true
)
public class APIFlattenVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @APIParam(required = false)
    private boolean full = true;

    @APIParam(required = false)
    private boolean dryRun;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public boolean isFull() {
        return full;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        APIFlattenVmInstanceMsg amsg = (APIFlattenVmInstanceMsg) msg;
        if (amsg.isDryRun()) {
            return null;
        }

        return new Result(amsg.getUuid(), VmInstanceVO.class);
    }

    public static APIFlattenVmInstanceMsg __example__() {
        APIFlattenVmInstanceMsg msg = new APIFlattenVmInstanceMsg();
        msg.setUuid(uuid());
        msg.setFull(true);
        msg.setDryRun(false);
        return msg;
    }
}
