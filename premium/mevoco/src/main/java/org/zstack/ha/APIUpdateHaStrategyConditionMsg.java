package org.zstack.ha;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author: DaoDao
 * @Date: 2023/4/4
 */
@RestRequest(
        path = "/ha-strategy-condition/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateHaStrategyConditionEvent.class
)
public class APIUpdateHaStrategyConditionMsg extends APIMessage {
    @APIParam(resourceType = HaStrategyConditionVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(required = false)
    private String state;

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static APIUpdateHaStrategyConditionMsg __example__() {
        APIUpdateHaStrategyConditionMsg msg = new APIUpdateHaStrategyConditionMsg();
        msg.uuid = uuid();
        msg.name = "new ha strategycondition name";
        msg.setState("Enable");
        return msg;
    }
}
