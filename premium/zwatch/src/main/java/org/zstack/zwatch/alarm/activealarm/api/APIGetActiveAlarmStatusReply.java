package org.zstack.zwatch.alarm.activealarm.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lining on 2020/10/19.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetActiveAlarmStatusReply extends APIReply {
    private List<ActiveAlarmStatus> statuses;

    public List<ActiveAlarmStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<ActiveAlarmStatus> statuses) {
        this.statuses = statuses;
    }

   public static APIGetActiveAlarmStatusReply __example__() {
       APIGetActiveAlarmStatusReply reply = new APIGetActiveAlarmStatusReply();
       ActiveAlarmStatus status = new ActiveAlarmStatus();
       status.setStatus("enable");
       status.setNamespace("ZStack/VM");
       reply.setStatuses(Arrays.asList(status));
        return reply;
    }

}
