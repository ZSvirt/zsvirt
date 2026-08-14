package org.zstack.storage.migration.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 8/30/17.
 */
@RestRequest(
        path = "/backup-storage/images/{imageUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIBackupStorageMigrateImageEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIBackupStorageMigrateImageMsg extends APIMessage implements StorageMigrationMessage {
    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String dstBackupStorageUuid;

    @APINoSee
    private String type;

    public static APIBackupStorageMigrateImageMsg __example__() {
        APIBackupStorageMigrateImageMsg msg = new APIBackupStorageMigrateImageMsg();
        msg.setImageUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());
        return msg;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
