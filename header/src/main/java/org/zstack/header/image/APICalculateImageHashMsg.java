package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.ImageHashAlgorithm;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:35 2023/11/10
 */
@RestRequest(
        path = "/images/{uuid}/actions",
        isAction = true,
        responseClass = APICalculateImageHashEvent.class,
        method = HttpMethod.PUT
)
public class APICalculateImageHashMsg extends APIMessage implements ImageMessage, APIAuditor {

    @APIParam(resourceType = ImageVO.class)
    private String uuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    @APIParam(required = false)
    private String algorithm = ImageHashAlgorithm.MD5.toString();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public static APICalculateImageHashMsg __example__() {
        APICalculateImageHashMsg msg = new APICalculateImageHashMsg();
        msg.setBackupStorageUuid(uuid());
        msg.setUuid(uuid());
        msg.setAlgorithm(ImageHashAlgorithm.MD5.toString());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APICalculateImageHashMsg)msg).uuid, ImageVO.class);
    }


    @Override
    public String getImageUuid() {
        return uuid;
    }
}
