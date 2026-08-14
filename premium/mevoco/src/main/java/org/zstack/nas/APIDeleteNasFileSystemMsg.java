package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestRequest(
        path = "/primary-storage/nas/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNasFileSystemEvent.class
)
public class APIDeleteNasFileSystemMsg extends APIDeleteMessage implements NasFileSystemMessage {
    @APIParam(resourceType = NasFileSystemVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getNasFileSystemUuid() {
        return uuid;
    }

    public static APIDeleteNasFileSystemMsg __example__() {
        APIDeleteNasFileSystemMsg msg = new APIDeleteNasFileSystemMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
