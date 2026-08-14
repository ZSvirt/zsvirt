package org.zstack.directory;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author shenjin
 * @date 2022/11/29 14:07
 * Move a directory to another directory and become its subdirectory
 */
@RestRequest(
        path = "/move/directory",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIMoveDirectoryEvent.class
)
public class APIMoveDirectoryMsg extends APIMessage implements DirectoryMessage, OperateDirectoryMessage {
    @APIParam(resourceType = DirectoryVO.class)
    private String targetParentUuid;
    @APIParam(resourceType = DirectoryVO.class)
    private String directoryUuid;

    public String getTargetParentUuid() {
        return targetParentUuid;
    }

    public void setTargetParentUuid(String targetParentUuid) {
        this.targetParentUuid = targetParentUuid;
    }

    @Override
    public String getDirectoryUuid() {
        return directoryUuid;
    }

    public void setDirectoryUuid(String directoryUuid) {
        this.directoryUuid = directoryUuid;
    }

    public static APIMoveDirectoryMsg __example__() {
        APIMoveDirectoryMsg msg = new APIMoveDirectoryMsg();
        msg.setDirectoryUuid(uuid());
        msg.setTargetParentUuid(uuid());
        return msg;
    }
}
