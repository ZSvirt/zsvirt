package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/clusters/{uuid}/actions",
        responseClass = APIUpdateClusterEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateClusterMsg extends APIMessage implements ClusterMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
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
    public String getClusterUuid() {
        return uuid;
    }
 
    public static APIUpdateClusterMsg __example__() {
        APIUpdateClusterMsg msg = new APIUpdateClusterMsg();
        msg.setUuid(uuid());
        msg.setName("cluster1");
        msg.setDescription("test");
        return msg;
    }
}
