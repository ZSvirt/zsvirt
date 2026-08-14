package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/database-backups/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRecoverDatabaseFromBackupEvent.class
)
public class APIRecoverDatabaseFromBackupMsg extends APIMessage {
    @APIParam(required = false, resourceType = DatabaseBackupVO.class)
    private String uuid;

    @APIParam(required = false)
    @NoLogging(type = NoLogging.Type.Uri)
    private String backupStorageUrl;

    @APIParam(required = false)
    private String backupInstallPath;

    @APIParam
    @NoLogging
    private String mysqlRootPassword;


    public String getBackupInstallPath() {
        return backupInstallPath;
    }

    public void setBackupInstallPath(String backupInstallPath) {
        this.backupInstallPath = backupInstallPath;
    }

    public String getBackupStorageUrl() {
        return backupStorageUrl;
    }

    public void setBackupStorageUrl(String backupStorageUrl) {
        this.backupStorageUrl = backupStorageUrl;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRecoverDatabaseFromBackupMsg __example__(){
        APIRecoverDatabaseFromBackupMsg msg = new APIRecoverDatabaseFromBackupMsg();
        msg.setBackupStorageUrl("ssh://root:password@localhost:22/zstack_bs");
        msg.setBackupInstallPath("zstore://zsbak/0ed599ec519249489475112a058bb93a");
        msg.setMysqlRootPassword("password");
        return msg;
    }

    public String getMysqlRootPassword() {
        return mysqlRootPassword;
    }

    public void setMysqlRootPassword(String mysqlRootPassword) {
        this.mysqlRootPassword = mysqlRootPassword;
    }
}
