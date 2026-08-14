package org.zstack.scheduler;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.scheduler.SchedulerJobHistoryInventory;
import org.zstack.header.scheduler.SchedulerJobVO;
import org.zstack.header.scheduler.SchedulerTriggerVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by MaJin on 2019/4/22.
 */

@RestResponse(allTo = "inventories")
public class APIQuerySchedulerJobHistoryReply extends APIQueryReply {

    private List<SchedulerJobHistoryInventory> inventories;

    public List<SchedulerJobHistoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SchedulerJobHistoryInventory> inventories) {
        this.inventories = inventories;
    }

    @SuppressWarnings("unchecked")
    public static APIQuerySchedulerJobHistoryReply __example__() {
        APIQuerySchedulerJobHistoryReply reply = new APIQuerySchedulerJobHistoryReply();
        SchedulerJobHistoryInventory inv = new SchedulerJobHistoryInventory();
        inv.setId(1);
        inv.setExecuteTime(900);
        inv.setSchedulerJobUuid(uuid(SchedulerJobVO.class));
        inv.setTargetResourceUuid(uuid(ResourceVO.class));
        inv.setTriggerUuid(uuid(SchedulerTriggerVO.class));
        inv.setStartTime(DocUtils.timestamp());
        inv.setSuccess(true);
        inv.setResultDump(JSONObjectUtil.toJsonString(map(
                e("apiId", uuid(ResourceVO.class)),
                e("success", true),
                e("id", uuid(APIQuerySchedulerJobHistoryReply.class)),
                e("createdTime", DocUtils.date)
        )));
        inv.setRequestDump(String.format("{\"bsUuid\":\"%s\"}", uuid(ResourceVO.class)));
        reply.setInventories(Collections.singletonList(inv));
        return reply;
    }
}
