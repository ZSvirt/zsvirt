package org.zstack.header.affinitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * Created by shixin on 11/15/2017.
 */
@RestRequest(
        path = "/affinity-groups",
        method = HttpMethod.POST,
        responseClass = APICreateAffinityGroupEvent.class,
        parameterName = "params"
)
public class APICreateAffinityGroupMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255, required = false)
    private String description;

    @APIParam(validValues = {"antiSoft", "antiHard"}, required = false)
    private String policy;

    @APIParam(validValues = {"host"}, required = false)
    private String type;

    @APIParam(resourceType = ZoneVO.class, required = false)
    private String zoneUuid;

    @APIParam(required = false)
    private String subType;

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

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public static APICreateAffinityGroupMsg __example__() {
        APICreateAffinityGroupMsg msg = new APICreateAffinityGroupMsg();

        msg.setName("vm-affinity-group");
        msg.setDescription("vm affinity group for test vms");
        msg.setPolicy("antiSoft");
        msg.setType("host");
        msg.setZoneUuid(uuid());
        msg.setSubType("vmSchedulingRule");

        return msg;
    }
}
