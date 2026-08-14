package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmInstanceVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/vm-backups/{groupUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRevertVmFromVmBackupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIRevertVmFromVmBackupMsg extends APIMessage implements APIAuditor {
    @APIParam
    private String groupUuid;

    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private String backupStorageUuid;


    @APIParam(required = false, validValues = {"InstantStart", "JustCreate", "CreateStopped"})
    private String strategy = VmCreationStrategy.CreateStopped.toString();

    @APINoSee
    private String vmInstanceUuid;

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getBackupStrogeUuid() {
        return backupStorageUuid;
    }

    public void setBackupStrogeUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public static APIRevertVmFromVmBackupMsg __example__() {
        APIRevertVmFromVmBackupMsg msg = new APIRevertVmFromVmBackupMsg();

        msg.setGroupUuid(uuid());
        msg.setBackupStrogeUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String vmInstanceUuid = ((APIRevertVmFromVmBackupMsg) msg).vmInstanceUuid;
        return vmInstanceUuid == null ? null : new Result(vmInstanceUuid, VmInstanceVO.class);
    }
}
