package org.zstack.directory;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author shenjin
 * @date 2022/11/29 14:06
 */
@RestRequest(
        path = "/update/directory",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateDirectoryEvent.class
)
public class APIUpdateDirectoryMsg extends APIMessage implements DirectoryMessage, OperateDirectoryMessage {
    @APIParam(resourceType = DirectoryVO.class)
    private String uuid;
    @APIParam(maxLength = 255)
    private String name;

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

    public static APIUpdateDirectoryMsg __example__() {
        APIUpdateDirectoryMsg msg = new APIUpdateDirectoryMsg();
        msg.setName("test");
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public String getDirectoryUuid() {
        return uuid;
    }
}
