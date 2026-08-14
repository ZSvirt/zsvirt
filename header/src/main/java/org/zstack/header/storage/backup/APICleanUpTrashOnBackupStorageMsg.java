package org.zstack.header.storage.backup;

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
        path = "/backup-storage/{uuid}/trash/actions",
        isAction = true,
        responseClass = APICleanUpTrashOnBackupStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpTrashOnBackupStorageMsg extends APIMessage implements BackupStorageMessage, APIBatchRequest {
    @APIParam(resourceType = BackupStorageVO.class)
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
    public String getBackupStorageUuid() {
        return uuid;
    }

    public Long getTrashId() {
        return trashId;
    }

    public void setTrashId(Long trashId) {
        this.trashId = trashId;
    }

    public static APICleanUpTrashOnBackupStorageMsg __example__() {
        APICleanUpTrashOnBackupStorageMsg msg = new APICleanUpTrashOnBackupStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public Result collectResult(APIMessage message, APIEvent rsp) {
        APICleanUpTrashOnBackupStorageEvent evt = (APICleanUpTrashOnBackupStorageEvent) rsp;
        return new Result(evt.getResults().size(),
                evt.getResults().stream()
                        .filter(TrashCleanupResult::isSuccess)
                        .toArray().length
        );
    }
}
