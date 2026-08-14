package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/primary-storage/{primaryStorageUuid}/usage/report",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageUsageReportReply.class
)
public class APIGetPrimaryStorageUsageReportMsg extends APISyncCallMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    @APIParam(required = false)
    private List<String> uris;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public static APIGetPrimaryStorageUsageReportMsg __example__() {
        APIGetPrimaryStorageUsageReportMsg msg = new APIGetPrimaryStorageUsageReportMsg();
        msg.setPrimaryStorageUuid(uuid());
        return msg;
    }
}
