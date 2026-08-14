package org.zstack.ovf.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.ovf.datatype.ImagePackageVO;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestRequest(
        path = "/image-packages/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateImagePackageEvent.class,
        isAction = true
)
public class APIUpdateImagePackageMsg extends APIMessage {
    @APIParam(resourceType = ImagePackageVO.class)
    private String uuid;

    @APIParam(required = false)
    private String name;

    @APIParam(required = false)
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

    public static APIUpdateImagePackageMsg __example__() {
        APIUpdateImagePackageMsg msg = new APIUpdateImagePackageMsg();
        msg.setUuid(uuid());
        msg.setName("ova-update");
        msg.setDescription("description-update");
        return msg;
    }
}
