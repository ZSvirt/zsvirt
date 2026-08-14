package org.zstack.header.cbt;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.cbt.CbtBackupConstant;

import java.util.concurrent.TimeUnit;

@Action(category = CbtBackupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/cbt-task/enable/{uuid}",
        method = HttpMethod.POST,
        responseClass = APIEnableCbtTaskEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIEnableCbtTaskMsg extends APIMessage implements CbtTaskMessage {
    @APIParam(emptyString = false)
    private String uuid;
    @APIParam(required = false)
    private String bitmapName;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getCbtTaskUuid() {
        return uuid;
    }

    public static APIEnableCbtTaskMsg __example__() {
        APIEnableCbtTaskMsg msg = new APIEnableCbtTaskMsg();
        msg.setUuid(uuid());
        msg.setBitmapName("");
        return msg;
    }

    public String getBitmapName() {
        return bitmapName;
    }

    public void setBitmapName(String bitmapName) {
        this.bitmapName = bitmapName;
    }
}
