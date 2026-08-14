package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 4/23/2015.
 */
@RestRequest(
        path = "/primary-storage/{uuid}/actions",
        responseClass = APIReconnectPrimaryStorageEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIReconnectPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

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
 
    public static APIReconnectPrimaryStorageMsg __example__() {
        APIReconnectPrimaryStorageMsg msg = new APIReconnectPrimaryStorageMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
