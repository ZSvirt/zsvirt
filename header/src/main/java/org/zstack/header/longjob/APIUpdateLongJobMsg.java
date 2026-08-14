package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 *  * Created on 2/3/2020
 *   */
@RestRequest(
        path = "/longjobs/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateLongJobEvent.class
)
public class APIUpdateLongJobMsg extends APIMessage implements LongJobMessage {
    @APIParam(resourceType = LongJobVO.class, scope = APIParam.SCOPE_MUST_OWNER)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIUpdateLongJobMsg __example__() {
        APIUpdateLongJobMsg msg = new APIUpdateLongJobMsg();
        msg.setUuid(uuid());
        msg.setName("new-name");
        msg.setDescription("new-description");
        return msg;
    }

    @Override
    public String getLongJobUuid() {
        return uuid;
    }
}

