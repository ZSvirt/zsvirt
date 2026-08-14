package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.io.Serializable;

@RestRequest(
        path = "/backup-storage/image-store",
        method = HttpMethod.POST,
        responseClass = APIAddImageStoreBackupStorageEvent.class,
        parameterName = "params"
)
@TagResourceType(BackupStorageVO.class)
public class APIAddImageStoreBackupStorageMsg extends APIAddBackupStorageMsg implements Serializable {
    @APIParam(maxLength = 255, emptyString = false)
    private String hostname;
    @APIParam(maxLength = 255)
    private String username;
    @APIParam(required = false, maxLength = 255, password = true)
    @NoLogging
    private String password;
    @APIParam(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

    @Override
    public String getType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
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
 
    public static APIAddImageStoreBackupStorageMsg __example__() {
        APIAddImageStoreBackupStorageMsg msg = new APIAddImageStoreBackupStorageMsg();

        msg.setName("ImageStore");
        msg.setUrl("/data/imagestore");
        msg.setHostname("192.168.1.8");
        msg.setUsername("admin");
        msg.setPassword("admin%pass");

        return msg;
    }

}
