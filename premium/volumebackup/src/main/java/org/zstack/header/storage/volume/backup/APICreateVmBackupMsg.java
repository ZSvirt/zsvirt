package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupMessage;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volumes/{rootVolumeUuid}/vm-backups",
        method = HttpMethod.POST,
        responseClass = APICreateVmBackupEvent.class,
        parameterName = "params"
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
public class APICreateVmBackupMsg extends APICreateMessage implements VolumeBackupMessage, APIMultiAuditor {
    /**
     * @desc volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class)
    private String rootVolumeUuid;

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

    @Override
    public String getVolumeUuid() {
        return getRootVolumeUuid();
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRemoteBackupStorageUuid() {
        return remoteBackupStorageUuid;
    }

    public void setRemoteBackupStorageUuid(String remoteBackupStorageUuid) {
        this.remoteBackupStorageUuid = remoteBackupStorageUuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public static APICreateVmBackupMsg __example__() {
        APICreateVmBackupMsg msg = new APICreateVmBackupMsg();

        msg.setName("backup-1");
        msg.setDescription("a critical volume backup");
        msg.setBackupStorageUuid(uuid());
        msg.setRootVolumeUuid(uuid());

        return msg;
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        List<APIAuditor.Result> results = new ArrayList<>();
        results.add(new APIAuditor.Result(rsp.isSuccess() ? ((APICreateVmBackupEvent)rsp).getInventories().get(0).getUuid() : "", VolumeBackupVO.class));

        String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                .eq(VolumeVO_.uuid, ((APICreateVmBackupMsg) msg).getRootVolumeUuid())
                .findValue();
        if (vmUuid != null) {
            results.add(new APIAuditor.Result(vmUuid, VmInstanceVO.class));
        }
        return results;
    }
}
