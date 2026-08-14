package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobGroupInventory;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQuerySchedulerJobGroupReply.class, inventoryClass = SchedulerJobGroupInventory.class)
@RestRequest(
        path = "/scheduler/jobgroups",
        optionalPaths = {"/scheduler/jobgroups/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySchedulerJobGroupReply.class
)
public class APIQuerySchedulerJobGroupMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=TestJobGroup", "state=Enabled");
    }
}
