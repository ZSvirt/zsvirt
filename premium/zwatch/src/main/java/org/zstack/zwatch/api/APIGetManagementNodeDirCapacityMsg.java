package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/zwatch/mn",
        method = HttpMethod.GET,
        responseClass = APIGetManagementNodeDirCapacityReply.class
)
public class APIGetManagementNodeDirCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ManagementNodeVO.class)
    private List<String> managementNodeUuids;

    public List<String> getManagementNodeUuids() {
        return managementNodeUuids;
    }

    public void setManagementNodeUuids(List<String> managementNodeUuids) {
        this.managementNodeUuids = managementNodeUuids;
    }

    public static APIGetManagementNodeDirCapacityMsg __example__() {
        APIGetManagementNodeDirCapacityMsg msg = new APIGetManagementNodeDirCapacityMsg();
        msg.setManagementNodeUuids(list(uuid(ManagementNodeVO.class)));
        return msg;
    }
}
