package org.zstack.autoscaling.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageStateEvent;

/**
 * Create by lining at 2018/10/10
 */
@RestRequest(
        path = "/autoscaling/groups/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeAutoScalingGroupStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeAutoScalingGroupStateMsg extends APIMessage implements AutoScalingGroupMessage {

    @APIParam(resourceType = AutoScalingGroupVO.class)
    private String uuid;

    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public APIChangeAutoScalingGroupStateMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }
 
    public static APIChangeAutoScalingGroupStateMsg __example__() {
        APIChangeAutoScalingGroupStateMsg msg = new APIChangeAutoScalingGroupStateMsg();

        msg.setUuid(uuid());
        msg.setStateEvent(PrimaryStorageStateEvent.disable.toString());

        return msg;
    }

    @Override
    public String getAutoScalingGroupUuid() {
        return uuid;
    }
}
