package org.zstack.header.storage.database.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageGlobalProperty;


@RestRequest(
        path = "/database-backups/image-store",
        method = HttpMethod.GET,
        responseClass = APIGetDatabaseBackupFromImageStoreReply.class
)
public class APIGetDatabaseBackupFromImageStoreMsg extends APISyncCallMessage {
    /**
     * @desc backup storage url, include username, password, hostname, sshPort, storagePath
     */
    @APIParam(emptyString = false)
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;

    /**
     * @desc image store registry port
     * port range (1,65535)
     */
    @APIParam(numberRange = {1, 65535}, required = false)
    private int registryPort = ImageStoreBackupStorageGlobalProperty.REGISTRY_PORT;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getRegistryPort() {
        return registryPort;
    }

    public void setRegistryPort(int registryPort) {
        this.registryPort = registryPort;
    }

    public static APIGetDatabaseBackupFromImageStoreMsg __example__() {
        APIGetDatabaseBackupFromImageStoreMsg msg = new APIGetDatabaseBackupFromImageStoreMsg();
        msg.setUrl("ssh://root:password@localhost:22/zstack_bs");
        return msg;
    }
}
