package org.zstack.header.zone;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/zones/{uuid}/actions",
        isAction = true,
        responseClass = APIUpdateZoneEvent.class,
        method = HttpMethod.PUT
)
public class APIUpdateZoneMsg extends APIMessage implements ZoneMessage {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = ZoneVO.class)
    private String uuid;
    @APIParam(required = false)
    private Boolean isDefault;

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
    public String getZoneUuid() {
        return uuid;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public static APIUpdateZoneMsg __example__() {
        APIUpdateZoneMsg msg = new APIUpdateZoneMsg();
        msg.setName("TestZone2");
        msg.setDescription("test second zone");
        msg.setUuid(uuid());
        msg.setDefault(true);
        return msg;
    }
}
