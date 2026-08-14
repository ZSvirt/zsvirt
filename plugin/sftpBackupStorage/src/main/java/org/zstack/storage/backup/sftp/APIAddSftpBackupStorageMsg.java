package org.zstack.storage.backup.sftp;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
/**
 * @api
 * add a sftp backup storage
 *
 * @category sftp backup storage
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg": {
"hostname": "localhost",
"username": "username",
"password": "password",
"port": "port",
"url": "nfs://test",
"name": "sftp",
"type": "SftpBackupStorage",
"session": {
"uuid": "6b01102e48ca4c8c9098f7684039e26a"
}
}
}
 *
 * @msg
 * {
"org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg": {
"hostname": "localhost",
"username": "username",
"password": "password",
"port": "port",
"url": "nfs://test",
"name": "sftp",
"type": "SftpBackupStorage",
"session": {
"uuid": "6b01102e48ca4c8c9098f7684039e26a"
},
"timeout": 1800000,
"id": "4f1fb24ae2db443f9ec486b0db84b899",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddBackupStorageEvent`
 */

@RestRequest(
        path = "/backup-storage/sftp",
        method = HttpMethod.POST,
        responseClass = APIAddSftpBackupStorageEvent.class,
        parameterName = "params"
)
public class APIAddSftpBackupStorageMsg extends APIAddBackupStorageMsg {
    @APIParam(maxLength = 255, emptyString = false)
    private String hostname;
    @APIParam(maxLength = 255)
    private String username;
    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;



    @Override
    public String getType() {
        return SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE;
    }

    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

 
    public static APIAddSftpBackupStorageMsg __example__() {
        APIAddSftpBackupStorageMsg msg = new APIAddSftpBackupStorageMsg();
        msg.setName("Sftp-Test");
        msg.setUrl("/data/sftp-data");
        msg.setHostname("192.168.1.8");
        msg.setUsername("admin");
        msg.setPassword("test1234");

        return msg;
    }

}
