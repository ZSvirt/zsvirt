package org.zstack.directory;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * @author shenjin
 * @date 2022/11/29 14:05
 */
@RestRequest(
        path = "/create/directory",
        method = HttpMethod.POST,
        responseClass = APICreateDirectoryEvent.class,
        parameterName = "params"
)
public class APICreateDirectoryMsg extends APICreateMessage implements OperateDirectoryMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false)
    private String parentUuid;
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    @APIParam(required = true)
    private String type;

    public static APICreateDirectoryMsg __example__() {
        APICreateDirectoryMsg ret = new APICreateDirectoryMsg();
        ret.name = "directory";
        ret.parentUuid = uuid();
        ret.zoneUuid = uuid();
        ret.type = "vminstance";
        return ret;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
