package org.zstack.storage.backup.imagestore;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

@RestRequest(
        path = "/backup-storage/image-store/actions",
        responseClass = APISetImageStoreBackupStorageQuotaEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APISetImageStoreBackupStorageQuotaMsg extends APIMessage {
    @APIParam(required = false, resourceType = ImageStoreBackupStorageVO.class)
    private List<String> uuids;

    @APIParam
    private long maxCapacity;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public long getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public static APISetImageStoreBackupStorageQuotaMsg __example__() {
        APISetImageStoreBackupStorageQuotaMsg msg = new APISetImageStoreBackupStorageQuotaMsg();
        msg.setUuids(list(uuid(ImageStoreBackupStorageVO.class)));
        msg.setMaxCapacity(1024L * 1024L * 1024L * 1024L);
        return msg;
    }
}
