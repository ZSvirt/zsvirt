package org.zstack.storage.ceph.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.io.Serializable;

/**
 * Created by Mei Lei on 6/3/2016.
 */
@RestRequest(
        path = "/backup-storage/ceph/mons/{monUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateCephBackupStorageMonEvent.class
)
public class APIUpdateCephBackupStorageMonMsg extends APIMessage implements BackupStorageMessage, Serializable {
    @APINoSee
    private String backupStorageUuid;

    @APIParam(resourceType = CephBackupStorageMonVO.class, emptyString = false)
    private String monUuid;

    @APIParam(maxLength = 255, required = false)
    private String hostname;

    @APIParam(maxLength = 255, required = false)
    private String sshUsername;

    @APIParam(maxLength = 255, required = false)
    @NoLogging
    private String sshPassword;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer monPort;

    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }


    public static APIUpdateCephBackupStorageMonMsg __example__() {
        APIUpdateCephBackupStorageMonMsg msg = new APIUpdateCephBackupStorageMonMsg();

        msg.setMonUuid(uuid());
        msg.setHostname("10.0.1.4");

        return msg;
    }
}


