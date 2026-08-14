package org.zstack.storage.ceph.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;
import org.zstack.storage.ceph.CephConstants;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@TagResourceType(BackupStorageVO.class)
@OverriddenApiParams({
        @OverriddenApiParam(field = "url", param = @APIParam(maxLength = 2048, required = false))
})
@RestRequest(
        path = "/backup-storage/ceph",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddBackupStorageEvent.class
)
public class APIAddCephBackupStorageMsg extends APIAddBackupStorageMsg implements Serializable {
    @APIParam(nonempty = false, emptyString = false)
    @NoLogging(type = NoLogging.Type.Uri)
    private List<String> monUrls;
    @APIParam(required = false, maxLength = 255)
    private String poolName;

    public String getUrl() {
        return "not used";
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public List<String> getMonUrls() {
        return monUrls;
    }

    public void setMonUrls(List<String> monUrls) {
        this.monUrls = monUrls;
    }

    @Override
    public String getType() {
        return CephConstants.CEPH_BACKUP_STORAGE_TYPE;
    }
 
    public static APIAddCephBackupStorageMsg __example__() {
        APIAddCephBackupStorageMsg msg = new APIAddCephBackupStorageMsg();

        msg.setName("My Ceph Backup Storage");
        msg.setMonUrls(Collections.singletonList("10.0.1.2"));
        msg.setPoolName("zs-images");

        return msg;
    }
}
