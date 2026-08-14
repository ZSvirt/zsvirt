package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@RestRequest(
        path = "/primary-storage/nas/mount/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateNasMountTargetEvent.class
)
public class APIUpdateNasMountTargetMsg extends APIMessage implements NasMountTargetMessage {
    @APIParam(resourceType = NasMountTargetVO.class)
    private String uuid;
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String name;
    @APIParam(maxLength = 1024, required = false)
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getNasMountTargetUuid() {
        return uuid;
    }

    public static APIUpdateNasMountTargetMsg __example__() {
        APIUpdateNasMountTargetMsg msg = new APIUpdateNasMountTargetMsg();
        msg.setUuid(uuid());
        msg.setName("modified");
        return msg;
    }
}
