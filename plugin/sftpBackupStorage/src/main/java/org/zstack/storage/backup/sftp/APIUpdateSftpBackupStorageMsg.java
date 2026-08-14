package org.zstack.storage.backup.sftp;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIUpdateBackupStorageEvent;
import org.zstack.header.storage.backup.APIUpdateBackupStorageMsg;

/**
 * Created by frank on 6/15/2015.
 */
@RestRequest(
        path = "/backup-storage/sftp/{uuid}/actions",
        responseClass = APIUpdateBackupStorageEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIUpdateSftpBackupStorageMsg extends APIUpdateBackupStorageMsg {
    @APIParam(maxLength = 255, required = false)
    private String username;
    @APIParam(maxLength = 255, required = false, password = true)
    @NoLogging
    private String password;
    @APIParam(maxLength = 255, required = false)
    private String hostname;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
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
 
    public static APIUpdateSftpBackupStorageMsg __example__() {
        APIUpdateSftpBackupStorageMsg msg = new APIUpdateSftpBackupStorageMsg();
        msg.setUsername("tester");
        msg.setSshPort(8000);
        msg.setHostname("192.168.0.1");
        msg.setPassword("test1234");
        msg.setUuid("76d39c6862b840a3aa4568d83db99022");

        return msg;
    }

}
