package org.zstack.imagereplicator;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Arrays;
import java.util.List;

@RestRequest(
        path = "/image-replication-groups/{replicationGroupUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddBackupStoragesToReplicationGroupEvent.class
)
public class APIAddBackupStoragesToReplicationGroupMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = ImageReplicationGroupVO.class)
    private String replicationGroupUuid;

    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    public String getReplicationGroupUuid() {
        return replicationGroupUuid;
    }

    public void setReplicationGroupUuid(String replicationGroupUuid) {
        this.replicationGroupUuid = replicationGroupUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public static APIAddBackupStoragesToReplicationGroupMsg __example__() {
        APIAddBackupStoragesToReplicationGroupMsg msg = new APIAddBackupStoragesToReplicationGroupMsg();
        msg.setReplicationGroupUuid(uuid());
        msg.setBackupStorageUuids(Arrays.asList(uuid(), uuid()));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddBackupStoragesToReplicationGroupMsg)msg).getReplicationGroupUuid() : "",
                ImageReplicationGroupVO.class);
    }
}
