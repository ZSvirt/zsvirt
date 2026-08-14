package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/primary-storage/{uuid}/takeover",
        responseClass = APITakeoverPrimaryStorageEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class APITakeoverPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

    @APIParam(required = false)
    private boolean dryRun;

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public static APITakeoverPrimaryStorageMsg __example__() {
        APITakeoverPrimaryStorageMsg msg = new APITakeoverPrimaryStorageMsg();
        msg.setUuid(uuid(PrimaryStorageVO.class));
        msg.setDryRun(false);
        return msg;
    }
}
