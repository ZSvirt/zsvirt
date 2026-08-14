package org.zstack.header.cbt;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.cbt.CbtBackupConstant;

@Action(category = CbtBackupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/cbt-task/disable/{uuid}",
        method = HttpMethod.POST,
        responseClass = APIDisableCbtTaskEvent.class,
        parameterName = "params"
)
public class APIDisableCbtTaskMsg extends APIMessage implements CbtTaskMessage {

    @APIParam(emptyString = false)
    private String uuid;

    @APIParam(required = false)
    private boolean force = false;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public static APIDisableCbtTaskMsg __example__() {
        APIDisableCbtTaskMsg msg = new APIDisableCbtTaskMsg();

        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getCbtTaskUuid() {
        return uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

}
