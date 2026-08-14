package org.zstack.storage.ceph.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 8/1/2015.
 */
@RestRequest(
        path = "/backup-storage/ceph/{uuid}/mons",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveMonFromCephBackupStorageEvent.class
)
public class APIRemoveMonFromCephBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = CephBackupStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> monHostnames;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonHostnames() {
        return monHostnames;
    }

    public void setMonHostnames(List<String> monHostnames) {
        this.monHostnames = monHostnames;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
 
    public static APIRemoveMonFromCephBackupStorageMsg __example__() {
        APIRemoveMonFromCephBackupStorageMsg msg = new APIRemoveMonFromCephBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setMonHostnames(Collections.singletonList("10.0.1.2"));

        return msg;
    }
}
