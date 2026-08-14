package org.zstack.directory;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.zone.ZoneVO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shenjin
 * @date 2022/11/29 14:09
 */
@RestRequest(
        path = "/move/resources/directory",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIMoveResourcesToDirectoryEvent.class
)
public class APIMoveResourcesToDirectoryMsg extends APIMessage implements DirectoryMessage {
    @APIParam(resourceType = ResourceVO.class, nonempty = true)
    private List<String> resourceUuids;
    @APIParam(resourceType = DirectoryVO.class)
    private String directoryUuid;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public String getDirectoryUuid() {
        return directoryUuid;
    }

    public void setDirectoryUuid(String directoryUuid) {
        this.directoryUuid = directoryUuid;
    }

    public static APIMoveResourcesToDirectoryMsg __example__() {
        APIMoveResourcesToDirectoryMsg msg = new APIMoveResourcesToDirectoryMsg();
        msg.directoryUuid = uuid();
        List<String> resourceUuids = new ArrayList<>();
        resourceUuids.add(uuid());
        resourceUuids.add(uuid());
        msg.resourceUuids = resourceUuids;
        return msg;
    }
}
