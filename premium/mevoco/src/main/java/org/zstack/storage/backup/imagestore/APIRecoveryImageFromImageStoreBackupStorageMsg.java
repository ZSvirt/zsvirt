package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.concurrent.TimeUnit;

/**
 * Created by mingjian.deng on 2017/9/12.
 */
@RestRequest(
        path = "/backup-storage/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRecoveryImageFromImageStoreBackupStorageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIRecoveryImageFromImageStoreBackupStorageMsg extends APIMessage {
    @APIParam(resourceType = ImageVO.class)
    private String uuid;
    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;
    @APIParam(resourceType = BackupStorageVO.class)
    private String dstBackupStorageUuid;

    @APIParam(maxLength = 255, emptyString = false)
    private String name;
    @APIParam(maxLength = 1024, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIRecoveryImageFromImageStoreBackupStorageMsg __example__() {
        APIRecoveryImageFromImageStoreBackupStorageMsg msg = new APIRecoveryImageFromImageStoreBackupStorageMsg();
        msg.setUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());
        msg.setName("disaster");
        msg.setDescription("disaster");

        return msg;
    }
}
