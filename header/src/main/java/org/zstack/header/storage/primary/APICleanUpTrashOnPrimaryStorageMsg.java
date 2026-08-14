package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.trash.TrashCleanupResult;
import org.zstack.header.message.APIBatchRequest;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/primary-storage/{uuid}/trash/actions",
        isAction = true,
        responseClass = APICleanUpTrashOnPrimaryStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpTrashOnPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage, APIBatchRequest {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;
    @APIParam(required = false)
    private Long trashId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public Long getTrashId() {
        return trashId;
    }

    public void setTrashId(Long trashId) {
        this.trashId = trashId;
    }

    public static APICleanUpTrashOnPrimaryStorageMsg __example__() {
        APICleanUpTrashOnPrimaryStorageMsg msg = new APICleanUpTrashOnPrimaryStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public Result collectResult(APIMessage message, APIEvent rsp) {
        APICleanUpTrashOnPrimaryStorageEvent evt = (APICleanUpTrashOnPrimaryStorageEvent) rsp;
        return new Result(evt.getResults().size(),
                evt.getResults().stream()
                        .filter(TrashCleanupResult::isSuccess)
                        .toArray().length
        );
    }
}
