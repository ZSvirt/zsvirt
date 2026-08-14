package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by kayo on 2018/4/13.
 */
@RestRequest(
        path = "/scheduler/jobs/candidates",
        method = HttpMethod.GET,
        responseClass = APIGetNoTriggerSchedulerJobsReply.class
)
public class APIGetNoTriggerSchedulerJobsMsg extends APISyncCallMessage {
    public static APIGetNoTriggerSchedulerJobsMsg __example__() {
        APIGetNoTriggerSchedulerJobsMsg msg = new APIGetNoTriggerSchedulerJobsMsg();
        return msg;
    }
}
