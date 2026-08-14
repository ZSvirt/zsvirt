package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/3/9.
 */
@RestRequest(
        path = "/primary-storage/nas/mount/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNasMountTargetEvent.class
)
public class APIDeleteNasMountTargetMsg extends APIDeleteMessage implements NasMountTargetMessage {
    @APIParam(resourceType = NasMountTargetVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getNasMountTargetUuid() {
        return uuid;
    }

    public static APIDeleteNasMountTargetMsg __example__() {
        APIDeleteNasMountTargetMsg msg = new APIDeleteNasMountTargetMsg();
        msg.setUuid(uuid());
        return msg;
    }
}