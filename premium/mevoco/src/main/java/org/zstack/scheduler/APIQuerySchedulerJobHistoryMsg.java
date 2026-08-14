package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.scheduler.SchedulerJobHistoryInventory;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/4/22.
 */
@AutoQuery(replyClass = APIQuerySchedulerJobHistoryReply.class, inventoryClass = SchedulerJobHistoryInventory.class)
@RestRequest(
        path = "/scheduler/job/history",
        method = HttpMethod.GET,
        responseClass = APIQuerySchedulerJobHistoryReply.class
)
public class APIQuerySchedulerJobHistoryMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("schedulerJobGroupUuid=7ae6456c0b01324dae6d4bef358a5772");
    }
}
