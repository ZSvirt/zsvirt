package org.zstack.zwatch.alarm.activealarm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/10/19.
 */
@RestRequest(
        path = "/zwatch/activealarms/status",
        method = HttpMethod.GET,
        responseClass = APIGetActiveAlarmStatusReply.class
)
public class APIGetActiveAlarmStatusMsg extends APISyncCallMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public static APIGetActiveAlarmStatusMsg __example__() {
        APIGetActiveAlarmStatusMsg msg = new APIGetActiveAlarmStatusMsg();
        msg.setAccountUuid(uuid());
        return msg;
    }

}
