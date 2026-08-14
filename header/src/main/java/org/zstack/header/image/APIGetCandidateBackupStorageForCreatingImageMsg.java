package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.SDK;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by xing5 on 2016/8/30.
 */
@RestRequest(
        path = "null",
        optionalPaths = {
                "/images/candidate-backup-storage",
                "/images/volumes/{volumeUuid}/candidate-backup-storage",
                "/images/volume-snapshots/{volumeSnapshotUuid}/candidate-backup-storage",
        },
        method = HttpMethod.GET,
        responseClass = APIGetCandidateBackupStorageForCreatingImageReply.class
)
@SDK(
        actionsMapping = {
                "GetCandidateBackupStorageForCreatingImage=/images/candidate-backup-storage",
                "GetBackupStorageForCreatingImageFromVolume=/images/volumes/{volumeUuid}/candidate-backup-storage",
                "GetBackupStorageForCreatingImageFromVolumeSnapshot=/images/volume-snapshots/{volumeSnapshotUuid}/candidate-backup-storage",
        }
)
public class APIGetCandidateBackupStorageForCreatingImageMsg extends APISyncCallMessage {
    @APIParam(resourceType = VolumeVO.class, required = false)
    private String volumeUuid;
    @APIParam(resourceType = VolumeSnapshotVO.class, required = false)
    private String volumeSnapshotUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }
 
    public static APIGetCandidateBackupStorageForCreatingImageMsg __example__() {
        APIGetCandidateBackupStorageForCreatingImageMsg msg = new APIGetCandidateBackupStorageForCreatingImageMsg();
        msg.setVolumeSnapshotUuid(uuid());
        msg.setVolumeUuid(uuid());
        return msg;
    }

}
