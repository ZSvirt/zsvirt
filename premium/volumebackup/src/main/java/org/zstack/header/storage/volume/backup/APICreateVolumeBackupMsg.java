package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupMessage;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.volume.VolumeVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volumes/{volumeUuid}/volume-backups",
        method = HttpMethod.POST,
        responseClass = APICreateVolumeBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APICreateVolumeBackupMsg extends APICreateMessage implements VolumeBackupMessage, APIAuditor {
    /**
     * @desc volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class)
    private String volumeUuid;

    /**
     * @desc ImageStore backup storage uuid
     */
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    /**
     * @desc backup name. Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;

    /**
     * @desc backup description. Max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    /**
     * @desc backup mode. Chosen automatically if not set.
     */
    @APIParam(required = false, validValues = {"full"})
    private String mode;

    @APINoSee
    private String remoteBackupStorageUuid;

    @APIParam(required = false, numberRange = {1024, Long.MAX_VALUE}, numberRangeUnit = {"Bps", "Bps"})
    private Long volumeReadBandwidth;

    @APIParam(required = false, numberRange = {1024, Long.MAX_VALUE}, numberRangeUnit = {"Bps", "Bps"})
    private Long volumeWriteBandwidth;

    @APIParam(required = false, numberRange = {1024, Long.MAX_VALUE}, numberRangeUnit = {"Bps", "Bps"})
    private Long networkReadBandwidth;

    @APIParam(required = false, numberRange = {1024, Long.MAX_VALUE}, numberRangeUnit = {"Bps", "Bps"})
    private Long networkWriteBandwidth;

    @APINoSee
    private String accountUuid;

    public Long getVolumeReadBandwidth() {
        return volumeReadBandwidth;
    }

    public void setVolumeReadBandwidth(Long volumeReadBandwidth) {
        this.volumeReadBandwidth = volumeReadBandwidth;
    }

    public Long getVolumeWriteBandwidth() {
        return volumeWriteBandwidth;
    }

    public void setVolumeWriteBandwidth(Long volumeWriteBandwidth) {
        this.volumeWriteBandwidth = volumeWriteBandwidth;
    }

    public Long getNetworkReadBandwidth() {
        return networkReadBandwidth;
    }

    public void setNetworkReadBandwidth(Long networkReadBandwidth) {
        this.networkReadBandwidth = networkReadBandwidth;
    }

    public Long getNetworkWriteBandwidth() {
        return networkWriteBandwidth;
    }

    public void setNetworkWriteBandwidth(Long networkWriteBandwidth) {
        this.networkWriteBandwidth = networkWriteBandwidth;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }

    public static APICreateVolumeBackupMsg __example__() {
        APICreateVolumeBackupMsg msg = new APICreateVolumeBackupMsg();

        msg.setName("backup-1");
        msg.setDescription("a critical volume backup");
        msg.setBackupStorageUuid(uuid());
        msg.setVolumeUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVolumeBackupEvent)rsp).getInventory().getUuid() : "", VolumeBackupVO.class);
    }
}
