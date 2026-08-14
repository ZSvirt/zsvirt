package org.zstack.nas;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@RestRequest(
        path = "/primary-storage/nas/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateNasFileSystemEvent.class
)
public class APIUpdateNasFileSystemMsg extends APIMessage implements NasFileSystemMessage {
    @APIParam(resourceType = NasFileSystemVO.class)
    private String uuid;
    @APIParam(maxLength = 255, emptyString = false, required = false)
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
    public String getNasFileSystemUuid() {
        return uuid;
    }

    public static APIUpdateNasFileSystemMsg __example__() {
        APIUpdateNasFileSystemMsg msg = new APIUpdateNasFileSystemMsg();
        msg.setUuid(uuid());
        msg.setName("modified");
        return msg;
    }
}
