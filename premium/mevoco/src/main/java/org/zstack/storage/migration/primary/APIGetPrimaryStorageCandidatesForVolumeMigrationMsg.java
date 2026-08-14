package org.zstack.storage.migration.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.migration.StorageMigrationMessage;

/**
 * Created by GuoYi on 9/21/17.
 */
@RestRequest(
        path = "/primary-storage/volumes/{volumeUuid}/migration-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetPrimaryStorageCandidatesForVolumeMigrationReply.class
)
public class APIGetPrimaryStorageCandidatesForVolumeMigrationMsg extends APISyncCallMessage implements StorageMigrationMessage {
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    public static APIGetPrimaryStorageCandidatesForVolumeMigrationMsg __example__() {
        APIGetPrimaryStorageCandidatesForVolumeMigrationMsg msg = new APIGetPrimaryStorageCandidatesForVolumeMigrationMsg();
        msg.setVolumeUuid(uuid());
        return msg;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
