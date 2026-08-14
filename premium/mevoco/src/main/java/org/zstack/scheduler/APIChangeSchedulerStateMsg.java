package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerStateEvent;

/**
 * Created by Mei Lei on 8/31/16.
 */
@RestRequest(
        path = "/schedulers/{uuid}",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIChangeSchedulerStateEvent.class
)
public class APIChangeSchedulerStateMsg  extends APIMessage implements SchedulerMessage  {
    @APIParam(resourceType = SchedulerJobVO.class)
    private String uuid;
    @APIParam(validValues={"enable", "disable"})
    private String stateEvent;

    public APIChangeSchedulerStateMsg() {
    }

    public APIChangeSchedulerStateMsg(String uuid, String stateEvent) {
        this.uuid = uuid;
        this.stateEvent = stateEvent;
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

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
 
    public static APIChangeSchedulerStateMsg __example__() {
        APIChangeSchedulerStateMsg msg = new APIChangeSchedulerStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SchedulerStateEvent.disable.toString());
        return msg;
    }
}
