package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/affinity-groups/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAffinityGroupEvent.class,
        isAction = true
)
public class APIUpdateAffinityGroupMsg extends APIMessage implements AffinityGroupMessage {
    @APIParam(resourceType = AffinityGroupVO.class, successIfResourceNotExisting = true)
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
 
    public static APIUpdateAffinityGroupMsg __example__() {
        APIUpdateAffinityGroupMsg msg = new APIUpdateAffinityGroupMsg();
        msg.setName("new name");
        msg.setDescription("desc");
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getAffinityGroupUuid() {
        return uuid;
    }
}
